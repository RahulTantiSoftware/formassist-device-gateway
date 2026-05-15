package com.form.device.gateway.service;
import com.form.device.gateway.dto.DeviceConnection;
import com.form.device.gateway.security.AuthTokenValidator;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Slf4j
public record DeviceWebSocketHandler(
        DeviceSessionManager sessionManager,
        AuthTokenValidator validator,
        StringRedisTemplate redis
) implements WebSocketHandler {
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        System.out.println("🔥 HANDLER HIT");
        MultiValueMap<String, String> queryParams =UriComponentsBuilder
                .fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams();
        String token = queryParams.getFirst("token");
        System.out.println("connection started .............");
        if (token == null || token.isEmpty()) {
            System.out.println("token param is null");
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

            if (redisValue == null || deviceCode==null) {
                System.out.println("device code can't be null");
                return session.close();
            }

            int tokenVersionFromDb = Integer.parseInt(redisValue);

            if (tokenVersionFromToken != tokenVersionFromDb) {
                return session.close(CloseStatus.POLICY_VIOLATION);
            }

            sessionManager.register(deviceCode, connection);

        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "";

            if ("token expired".equalsIgnoreCase(message)) {
                log.warn("❌ Token expired");

                return session.close(CloseStatus.POLICY_VIOLATION);
            }

            log.error("❌ WS AUTH FAILED: {}", message, e);

            return session.close(CloseStatus.NOT_ACCEPTABLE);
        }

        return session.receive()
                .doOnNext(msg -> {
                    connection.setLastHeartbeat(System.currentTimeMillis());
                })
                .doFinally(signal -> sessionManager.remove(deviceCode))
                .then();
    }
}