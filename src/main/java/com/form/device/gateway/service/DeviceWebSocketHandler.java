package com.form.device.gateway.service;
import com.form.device.gateway.dto.DeviceConnection;
import com.form.device.gateway.security.AuthTokenValidator;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.socket.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
public record DeviceWebSocketHandler(
        DeviceSessionManager sessionManager,
        AuthTokenValidator validator,
        StringRedisTemplate redis
) implements WebSocketHandler {
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        System.out.println("🔥 HANDLER HIT");
        List<String> protocols = session.getHandshakeInfo()
                .getHeaders()
                .get("Sec-WebSocket-Protocol");

        if (protocols == null || protocols.isEmpty()) {
            return session.close();
        }

        String token = protocols.getFirst();

        if (token.isEmpty()) {
            System.out.println("token param is null");
            return session.close();
        }


        DeviceConnection connection;
        DeviceConnection tryConnection=null;
        String deviceCode;
        try {
            JWTClaimsSet claims       = validator.validate(token);
            Long userId               = Long.valueOf(claims.getSubject());
            int tokenVersionFromToken = claims.getIntegerClaim("tokenVersion");
            deviceCode                = claims.getStringClaim("key");
            String redisValue         = redis.opsForValue().get("token_version:" + userId);
            connection                = tryConnection = new DeviceConnection(session,deviceCode,redis);
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
            if(tryConnection!=null) tryConnection.updateHeartbeat("closed");
            return session.close(CloseStatus.NOT_ACCEPTABLE);
        }

        return session.receive()
                .doOnNext(msg -> {
                    connection.updateHeartbeat("active");
                })
                .doFinally(signal -> sessionManager.remove(deviceCode))
                .then();

    }
}