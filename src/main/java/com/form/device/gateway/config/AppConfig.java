package com.form.device.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.form.device.gateway.security.AuthTokenValidator;
import com.form.device.gateway.service.DeviceSessionManager;
import com.form.device.gateway.service.DeviceWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.*;

import java.util.Map;

@Configuration
@EnableWebFlux
public class AppConfig {

    @Bean
    public HandlerMapping webSocketMapping(DeviceSessionManager manager, AuthTokenValidator validator, StringRedisTemplate redis) {
        Map<String, WebSocketHandler> map = Map.of(
                "/ws", new DeviceWebSocketHandler(manager,validator,redis)
        );

        return new SimpleUrlHandlerMapping(map, -1);
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

}