package com.form.device.gateway.service;
import com.form.device.gateway.dto.DeviceConnection;
import com.form.device.gateway.security.AuthTokenValidator;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.socket.*;
import reactor.core.publisher.Mono;

public record DeviceWebSocketHandler(
        DeviceSessionManager sessionManager,
        AuthTokenValidator validator,
        StringRedisTemplate redis
) implements WebSocketHandler {
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String authHeader = session.getHandshakeInfo().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return session.close();
        }

        String token = authHeader.substring(7).trim();
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