package com.winitech.rnd.configuration;

import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.annotation.*;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.healthcheck.HealthCheckService;
import com.linecorp.armeria.server.logging.AccessLogWriter;
import com.linecorp.armeria.server.logging.LoggingService;
import com.linecorp.armeria.spring.ArmeriaServerConfigurator;
import com.winitech.rnd.converter.FetchFromMyBatis;
import com.winitech.rnd.decorator.HttpBody2Params;
import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ArmeriaServerConfiguration {
    private final SqlSessionFactory sqlSessionFactory;
    private final FetchFromMyBatis converter;
    private final HttpBody2Params decorator;

    @Bean
    public ArmeriaServerConfigurator armeriaServerConfigurator() {

        return new ArmeriaServerConfigurator() {

            @SneakyThrows
            @Override
            public void configure(ServerBuilder serverBuilder) {
                // Java Network Application Framework(Use Armeria)
                serverBuilder.decorator(LoggingService.newDecorator());
                serverBuilder.accessLogWriter(AccessLogWriter.combined(), false);

                // Java Bytecode Manipulation lib(Use Javassist)
                ClassPool pool = ClassPool.getDefault();
                pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));

                // mybatis get mappedStatements
                org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
                Collection<?> trimMappedStatement = configuration.getMappedStatements();
                trimMappedStatement.removeIf((Predicate<Object>) o -> !(o instanceof MappedStatement));
                Collection<MappedStatement> mappedStatements = (Collection<MappedStatement>) trimMappedStatement;

                // get namespaces
                List<String> namespaces = mappedStatements.stream()
                        .filter(mappedStatement -> mappedStatement.getId().contains("."))
                        .map(mappedStatement -> substringBeforeLast(mappedStatement.getId(), '.'))
                        .distinct().collect(Collectors.toList());

                for (String namespace : namespaces) {
                    List<String> ids = mappedStatements.stream()
                         .filter(mappedStatement -> mappedStatement.getId().startsWith(namespace+".")
                                 && !StringUtils.endsWithIgnoreCase(mappedStatement.getId(), SelectKeyGenerator.SELECT_KEY_SUFFIX))
                         .map(mappedStatement -> mappedStatement.getId())
                         .distinct().collect(Collectors.toList());
                    CtClass ctClass = genClass(pool, configuration, ids, namespace);

                    Class clazz = ctClass.toClass();
                    Object obj = clazz.getDeclaredConstructor().newInstance();
                    serverBuilder.annotatedService(obj, httpService -> httpService.decorate(decorator), converter);

                }

                serverBuilder
                        .serviceUnder("/docs", new DocService())
                        .service("/healthcheck", HealthCheckService.of());
            }
        };
    }

    private CtClass genClass(ClassPool pool, org.apache.ibatis.session.Configuration configuration, List<String> ids, String classNm)
            throws Exception {
        // Set import
        pool.importPackage("java.util");
        pool.importPackage("com.winitech.rnd.converter");

        // Set Class name
        CtClass ctClass = pool.makeClass(classNm);

        // Add Class Annotation
        AnnotationsAttribute classAnnotationAttr = genAnnotationsAttribute(ProducesJson.class.getName(), ctClass);
        ctClass.getClassFile().addAttribute(classAnnotationAttr);

        // Create Methods
        for (String id:ids) {
            CtClass returnType = pool.get("com.winitech.rnd.converter.WiniResponse");
            CtClass defaultParamType = pool.get("java.lang.String");

            MappedStatement mappedStatement = configuration.getMappedStatement(id);
            String[] keyArray ={};
            // Extract insert->selectKey->keyProperty
            if (mappedStatement.getKeyGenerator() instanceof SelectKeyGenerator) {
                MappedStatement selectKeyStatement = configuration.getMappedStatement(id + SelectKeyGenerator.SELECT_KEY_SUFFIX);
                keyArray = selectKeyStatement.getKeyProperties();
            }

            final List selectKeys = Arrays.asList(keyArray);

            List<ParameterMapping> allParamMappings = mappedStatement.getBoundSql(new HashMap<>()).getParameterMappings();
            // Deduplication
            List<ParameterMapping> paramMappings = allParamMappings.stream().filter(paramMapping -> !selectKeys.contains(paramMapping.getProperty()))
                    .filter(distinctByKey(parameterMapping -> parameterMapping.getProperty())).collect(Collectors.toList());
            String methodName = substringAfterLast(id, '.');
            int fetchSize = mappedStatement.getFetchSize() == null ? 0 : mappedStatement.getFetchSize();

            // Add Params
            int paramCnt = paramMappings.size();
            CtClass[] params = new CtClass[paramCnt];
            for (int i = 0; i < paramCnt ; i++) {
                String clazzNm = paramMappings.get(i).getJavaType().getName();
                params[i] = clazzNm.equals("java.lang.Object") ? defaultParamType : pool.get(clazzNm);
            }

            // Create Method
            CtMethod ctMethod = new CtMethod(returnType, methodName, params, ctClass);
            ctMethod.setModifiers(Modifier.PUBLIC);
            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("  HashMap param = new HashMap();");
            for (int i = 0; i < paramCnt; i++) {
                body.append("param.put(\"" + paramMappings.get(i).getProperty() + "\", " + "$" + (i+1) + ");");
            }
            body.append("  String id = \"" + id + "\";");
            body.append("  int fetchSize = " + fetchSize + ";");
            body.append("  List keys = new ArrayList();");
            for (int i = 0; i < selectKeys.size() ; i++) {
                body.append("keys.add(\"" + selectKeys.get(i) + "\");");
            }
            body.append("  String sqlCmd = \"" + mappedStatement.getSqlCommandType().name() + "\";");
            body.append("  return new WiniResponse(id, param, fetchSize, keys, sqlCmd);");
            body.append("}");
            ctMethod.setBody(body.toString());

            // Add Method Annotation
            Class annClass;
            switch (mappedStatement.getSqlCommandType()) {
                case SELECT: annClass = Get.class; break;
                case INSERT: annClass = Post.class; break;
                case UPDATE: annClass = Put.class; break;
                case DELETE: annClass = Delete.class; break;
                default: throw new Exception("SqlCommandType does not belong to SELECT, INSERT, UPDATE, or DELETE.");
            }
            String path = "/" + id.replace('.','/');
            AnnotationsAttribute methodAnnotation = genAnnotationsAttribute(annClass.getName(), "value", path, ctMethod);
            ctMethod.getMethodInfo().addAttribute(methodAnnotation);

            // Add Param Annotation
            if(paramCnt > 0) {
                ParameterAnnotationsAttribute parameterAnnotationsAttribute = new ParameterAnnotationsAttribute(
                        ctMethod.getMethodInfo().getConstPool(), ParameterAnnotationsAttribute.visibleTag);
                Annotation[][] annotations = new Annotation[paramCnt][1];
                for (int i = 0; i < paramCnt ; i++) {
                    Annotation paramAnno = new Annotation(Param.class.getName(), ctMethod.getMethodInfo().getConstPool());
                    paramAnno.addMemberValue("value", new StringMemberValue(paramMappings.get(i).getProperty(),
                            ctMethod.getMethodInfo().getConstPool()));
                    annotations[i][0] = paramAnno;
                }
                parameterAnnotationsAttribute.setAnnotations(annotations);
                ctMethod.getMethodInfo().addAttribute(parameterAnnotationsAttribute);
            }

            // Add Method to Class
            ctClass.addMethod(ctMethod);
        }
        return ctClass;
    }

    private <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> map = new HashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private AnnotationsAttribute genAnnotationsAttribute(String typeName, String name, String value, CtMethod method) {
        MethodInfo methodInfoGetEid = method.getMethodInfo();

        return getAnnotationsAttribute(typeName, name, value, methodInfoGetEid.getConstPool());
    }

    private AnnotationsAttribute genAnnotationsAttribute(String typeName, CtClass clazz) {
        ClassFile classFile = clazz.getClassFile();
        ConstPool cp = classFile.getConstPool();
        Annotation annotation = getAnnotation(typeName, cp);
        AnnotationsAttribute attributeNew = new AnnotationsAttribute(cp, AnnotationsAttribute.visibleTag);
        attributeNew.setAnnotation(annotation);

        return attributeNew;
    }

    private AnnotationsAttribute getAnnotationsAttribute(String typeName, String name, String value, ConstPool cp) {
        Annotation annotation = getAnnotation(typeName, name, value, cp);
        AnnotationsAttribute attributeNew = new AnnotationsAttribute(cp, AnnotationsAttribute.visibleTag);
        attributeNew.setAnnotation(annotation);

        return attributeNew;
    }

    private Annotation getAnnotation(String typeName, ConstPool cp) {
        Annotation annotation = new Annotation(typeName, cp);

        return annotation;
    }

    private Annotation getAnnotation(String typeName, String name, String value, ConstPool cp) {
        Annotation annotation = new Annotation(typeName, cp);
        annotation.addMemberValue(name, new StringMemberValue(value, cp));

        return annotation;
    }

    private String substringAfterLast(String value, char delim) {
        int pos = value.lastIndexOf(delim);
        if (pos >= 0) {
            return value.substring(pos + 1);
        }
        return null;
    }

    private String substringBeforeLast(String value, char delim) {
        int pos = value.lastIndexOf(delim);
        if (pos >= 0) {
            return value.substring(0, pos);
        }
        return null;
    }
}
