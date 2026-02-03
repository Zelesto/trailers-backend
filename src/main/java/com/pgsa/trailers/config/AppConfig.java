package com.pgsa.trailers.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Value("${rest.template.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${rest.template.read-timeout:10000}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Configure timeout
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        restTemplate.setRequestFactory(factory);

        // Optional: Add interceptors for logging
        /*
        restTemplate.getInterceptors().add((request, body, execution) -> {
            log.debug("Request URL: {}", request.getURI());
            log.debug("Request Method: {}", request.getMethod());
            log.debug("Request Headers: {}", request.getHeaders());
            return execution.execute(request, body);
        });
        */

        return restTemplate;
    }
}