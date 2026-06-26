package com.example.demo.order;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class RestClientConfig {

    @Bean
    RestClient restClient(Tracer tracer) {
        return RestClient.builder()
                .defaultHeader("User-Agent", "order-service")
                .requestInterceptor((request, body, execution) -> {
                    Span currentSpan = tracer.currentSpan();
                    if (currentSpan != null) {
                        request.getHeaders().set("traceparent", traceparent(currentSpan));
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    private String traceparent(Span span) {
        return "00-" + span.context().traceId() + "-" + span.context().spanId() + "-01";
    }
}
