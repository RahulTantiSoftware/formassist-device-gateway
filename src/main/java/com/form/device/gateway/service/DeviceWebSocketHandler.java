package com.form.device.gateway.service;
import com.form.device.gateway.dto.DeviceConnection;
import com.form.device.gateway.security.AuthTokenValidator;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

public record DeviceWebSocketHandler(
        DeviceSessionManager sessionManager,
        AuthTokenValidator validator,
        StringRedisTemplate redis
) implements WebSocketHandler {
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        MultiValueMap<String, String> queryParams =UriComponentsBuilder
                .fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams();
        String token = queryParams.getFirst("token");

        if (token == null || token.isEmpty()) {
            return session.close();
        }

        DeviceConnection connection = new DeviceConnection(session);
        String deviceCode;
        try {
            JWTClaimsSet claims       = validator.validate(token);
            Long userId               = Long.valueOf(claims.getSubject());
            int tokenVersionFromToken = claims.getIntegerClaim("tokenVersion");
            deviceCode                = claims.getStringClaim("key");
            String redisValue         = redis.opsForValue().get("token_version:" + userId);

            if (redisValue == null) {
                return session.close();
            }

            int tokenVersionFromDb = Integer.parseInt(redisValue);

            if (tokenVersionFromToken != tokenVersionFromDb) {
                return session.close();
            }

            sessionManager.remove(deviceCode);

            sessionManager.register(deviceCode, connection);

        } catch (Exception e) {
            return session.close();
        }

        return session.receive()
                .doOnNext(msg -> {
                    connection.setLastHeartbeat(System.currentTimeMillis());
                })
                .doFinally(signal -> sessionManager.remove(deviceCode))
                .then();
    }
}