package com.winitech.rnd.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.internal.shaded.guava.collect.Maps;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.ResponseConverterFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Controller;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FetchFromMyBatis implements ResponseConverterFunction {
    private final SqlSessionFactory sqlSessionFactory;
    private final ObjectMapper objectMapper;

    @Override
    public HttpResponse convertResponse(ServiceRequestContext ctx,
                                        ResponseHeaders headers,
                                        @Nullable Object result,
                                        HttpHeaders trailers) throws Exception {
        if(result instanceof WiniResponse) {
            String sqlCmd = ((WiniResponse) result).getSqlCmd();

            switch (sqlCmd) {
                case "SELECT": return executeSelect(ctx, (WiniResponse) result);
                case "INSERT": return executeInsert(ctx, (WiniResponse) result);
                case "UPDATE": return executeUpdate(ctx, (WiniResponse) result);
                case "DELETE": return executeDelete(ctx, (WiniResponse) result);
                default: throw new Exception("SqlCommandType does not belong to SELECT, INSERT, UPDATE, or DELETE.");
            }
        }
        return ResponseConverterFunction.fallthrough();
    }

    private HttpResponse executeSelect(ServiceRequestContext ctx, WiniResponse result) {
        String id = result.getId();
        Map params = result.getParams();
        int fetchSize = result.getFetchSize();

        final CompletableFuture<HttpResponse> fetchFromDb = CompletableFuture.supplyAsync(() -> {
            try(SqlSession sqlSession = sqlSessionFactory.openSession()) {
                Object obj = fetchSize == 1 ? sqlSession.selectOne(id, params) : sqlSession.selectList(id, params);
                String json = obj != null ? objectMapper.writeValueAsString(obj) : "{}";
                return HttpResponse.of(MediaType.JSON_UTF_8, json);
            } catch(PersistenceException | JsonProcessingException e) {
                log.error(e.getMessage());
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.ANY_TEXT_TYPE, e.getMessage());
            }
        }, ctx.blockingTaskExecutor());
        return HttpResponse.from(fetchFromDb);
    }

    private HttpResponse executeInsert(ServiceRequestContext ctx, WiniResponse result) {
        String id = result.getId();
        Map params = result.getParams();
        List<String> keys = result.getKeys();
        Map<String, Object> map = Maps.newHashMap();

        final CompletableFuture<HttpResponse> fetchFromDb = CompletableFuture.supplyAsync(() -> {
            try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
                sqlSession.insert(id, params);
                for (String key : keys) {
                    map.put(key, params.get(key));
                }
                String json = objectMapper.writeValueAsString(map);
                return HttpResponse.of(MediaType.JSON_UTF_8, json);
            } catch(PersistenceException | JsonProcessingException e) {
                log.error(e.getMessage());
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.PLAIN_TEXT_UTF_8, e.getMessage());
            }
        }, ctx.blockingTaskExecutor());
        return HttpResponse.from(fetchFromDb);
    }

    private HttpResponse executeUpdate(ServiceRequestContext ctx, WiniResponse result) {
        String id = result.getId();
        Map params = result.getParams();
        Map<String, Object> map = Maps.newHashMap();

        final CompletableFuture<HttpResponse> fetchFromDb = CompletableFuture.supplyAsync(() -> {
            try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
                sqlSession.update(id, params);


                map.put("result", true);
                String json = objectMapper.writeValueAsString(map);
                return HttpResponse.of(MediaType.JSON_UTF_8, json);
            } catch(PersistenceException | JsonProcessingException e) {
                log.error(e.getMessage());
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.PLAIN_TEXT_UTF_8, e.getMessage());
            }
        }, ctx.blockingTaskExecutor());
        return HttpResponse.from(fetchFromDb);
    }

    private HttpResponse executeDelete(ServiceRequestContext ctx, WiniResponse result) {
        String id = result.getId();
        Map params = result.getParams();
        Map<String, Object> map = Maps.newHashMap();

        final CompletableFuture<HttpResponse> fetchFromDb = CompletableFuture.supplyAsync(() -> {
            try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
                sqlSession.delete(id, params);

                map.put("result", true);
                String json = objectMapper.writeValueAsString(map);
                return HttpResponse.of(MediaType.JSON_UTF_8, json);
            } catch(PersistenceException | JsonProcessingException e) {
                log.error(e.getMessage());
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.PLAIN_TEXT_UTF_8, e.getMessage());
            }
        }, ctx.blockingTaskExecutor());
        return HttpResponse.from(fetchFromDb);
    }
}
