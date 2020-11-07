package com.winitech.rnd.decorator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.internal.shaded.guava.base.Strings;
import com.linecorp.armeria.server.DecoratingHttpServiceFunction;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServiceRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HttpBody2Params implements DecoratingHttpServiceFunction {
    private final ObjectMapper objectMapper;

    @Override
    public HttpResponse serve(HttpService delegate, ServiceRequestContext ctx, HttpRequest req) throws Exception {
        if(req.method() == HttpMethod.GET || req.method() == HttpMethod.DELETE) return delegate.serve(ctx, req);

        // req.method() in HttpMethod.POST, HttpMethod.PUT
        CompletableFuture<HttpResponse> httpResponseCompletableFuture = req.aggregate().thenApply(aggregatedHttpRequest -> {
            try {
                String body = aggregatedHttpRequest.contentUtf8();
                String newPath;
                Map<String, Object> map = objectMapper.readValue(body, Map.class);
                if(map.size() > 0) {
                    String newQuery = map.keySet().stream().map(key -> key + "=" + map.get(key)).collect(Collectors.joining("&"));
                    String path = req.uri().getPath();
                    String oldQuery = req.uri().getQuery();
                    newPath = path + "?" + newQuery;
                    if(!Strings.isNullOrEmpty(oldQuery)) {
                        newPath += "&" + oldQuery;
                    }
                } else {
                    newPath = req.path();
                }

                HttpRequest newReq = req.withHeaders(req.headers().toBuilder().set(":path", newPath).build());
                final ServiceRequestContext newCtx = ServiceRequestContext.of(newReq);

                return delegate.serve(newCtx, newReq);
            } catch (Exception e) {
                log.error(e.getMessage());
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.PLAIN_TEXT_UTF_8, e.getMessage());
            }
        });

        return HttpResponse.from(httpResponseCompletableFuture);
    }
}
