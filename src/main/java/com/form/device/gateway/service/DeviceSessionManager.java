package com.form.device.gateway.service;

import com.form.device.gateway.dto.DeviceConnection;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class DeviceSessionManager {
    private final Map<String, DeviceConnection> connections = new ConcurrentHashMap<>();

    public void register(String deviceCode, DeviceConnection connection) {
        connections.put(deviceCode, connection);
    }

    public void remove(String deviceId) {
        DeviceConnection connection = connections.remove(deviceId);
        close(connection);
    }

    private void close(DeviceConnection connection) {
        if (connection == null) return;
        connection.getSession()
                .close()
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    @PostConstruct
    public void startHeartbeatCheck() {
        Flux.interval(Duration.ofSeconds(30))
                .doOnNext(tick -> checkConnections())
                .subscribe();
    }

    private void checkConnections() {
        long now = System.currentTimeMillis();
        connections.entrySet().removeIf(entry -> {
            DeviceConnection conn = entry.getValue();
            if (now - conn.getLastHeartbeat() > 2 * 60 * 1000) {
                close(conn);
                return true;
            }
            return false;
        });
    }

    public void send(String deviceCode, String message) {
        DeviceConnection connection = connections.get(deviceCode);
        if (connection != null && connection.getSession().isOpen()) {
            connection.getSession().send(Mono.just(connection.getSession().textMessage(message))).subscribe();
        }
    }
}