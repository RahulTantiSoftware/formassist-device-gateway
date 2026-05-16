package com.form.device.gateway.dto;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.time.Duration;

public record DeviceConnection(
        WebSocketSession session,
        String deviceCode,
        StringRedisTemplate stringRedisTemplate
) {

    public void updateHeartbeat(String status) {
        stringRedisTemplate.opsForValue().set("lastHeartbeat:" + deviceCode, status, Duration.ofSeconds(120));
    }

    public boolean isHeartbeatActive() {
        String status = stringRedisTemplate.opsForValue().get("lastHeartbeat:" + deviceCode);
        return status != null && status.equalsIgnoreCase("active");
    }

    public void stopHeartbeat() {
        stringRedisTemplate.delete("lastHeartbeat:" + deviceCode);
    }
}
