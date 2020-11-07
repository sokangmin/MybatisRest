package com.winitech.rnd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.Default;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


@Slf4j
@Component
public class Test {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;
    @Autowired
    private ObjectMapper objectMapper;

    @Get("/greet2") // `:name` style is also available
    public HttpResponse greet(@Param(value = "name") @Default String nm, @NotNull ServiceRequestContext ctx) {
        final CompletableFuture fetchFromDb = CompletableFuture.supplyAsync((Supplier) () -> {
            SqlSession sqlSession = sqlSessionFactory.openSession();
            HashMap map = new HashMap<>();
            map.put("stnid", "108");
            map.put("tmseq", "98");
            Object o = sqlSession.selectList("winitech.selectT3", map);

            //sqlSession.close();
            try {
                String s = objectMapper.writeValueAsString(o);
                return HttpResponse.of(MediaType.JSON_UTF_8, s);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
                e.printStackTrace();
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.ANY_TEXT_TYPE, e.getMessage());
            }
        });

        return HttpResponse.from(fetchFromDb);
    }
}

