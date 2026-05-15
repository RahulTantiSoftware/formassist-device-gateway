package com.form.device.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.form.device.gateway.security.AuthTokenValidator;
import com.form.device.gateway.service.DeviceSessionManager;
import com.form.device.gateway.service.DeviceWebSocketHandler;
import io.micrometer.common.lang.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.*;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Configuration
@EnableWebFlux
public class AppConfig {

    @Bean
    public HandlerMapping webSocketMapping(DeviceSessionManager manager, AuthTokenValidator validator, StringRedisTemplate redis) {
        Map<String, WebSocketHandler> map = Map.of(
                "/ws/**", new DeviceWebSocketHandler(manager,validator,redis)
        );

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public WebSocketService webSocketService() {
        return new HandshakeWebSocketService(new ReactorNettyRequestUpgradeStrategy() {
            @Override
            public Mono<Void> upgrade(ServerWebExchange exchange,
                                      WebSocketHandler handler,
                                      @Nullable String subProtocol,
                                      Supplier<HandshakeInfo> handshakeInfoFactory) {

                List<String> protocols = exchange.getRequest()
                        .getHeaders()
                        .get("Sec-WebSocket-Protocol");

                String protocol = (protocols != null && !protocols.isEmpty())
                        ? protocols.get(0)
                        : null;

                return super.upgrade(exchange, handler, protocol, handshakeInfoFactory);
            }
        });
    }


    @Bean
    public WebSocketHandlerAdapter handlerAdapter(WebSocketService webSocketService) {
        return new WebSocketHandlerAdapter(webSocketService);
    }


}