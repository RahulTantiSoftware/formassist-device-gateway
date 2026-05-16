package com.form.device.gateway.service;

import com.form.device.gateway.dto.DeviceConnection;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class DeviceSessionManager {
    private final Map<String, DeviceConnection> connections = new ConcurrentHashMap<>();

    public void register(String deviceCode, DeviceConnection connection) {
        DeviceConnection existing = connections.get(deviceCode);

        if (existing != null) {
            close(existing);
        }
        connections.put(deviceCode, connection);
        connection.updateHeartbeat("active");
    }

    public void remove(String deviceId) {
        DeviceConnection connection = connections.remove(deviceId);
        close(connection);
    }

    private void close(DeviceConnection connection) {
        if (connection == null) return;

        WebSocketSession session = connection.session();
        if (session == null || !session.isOpen()) return;

        session.close()
                .doOnSuccess(v ->{connection.stopHeartbeat();System.out.println("Session closed for device");})
                .doOnError(e -> System.out.println(MessageFormat.format("Error closing session: {0}", e.getMessage())))
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
        connections.entrySet().removeIf(entry -> {
            DeviceConnection conn = entry.getValue();
            if (!conn.isHeartbeatActive()) {
                close(conn);
                return true;
            }
            return false;
        });
    }

    public void send(String deviceCode, String message) {
        DeviceConnection connection = connections.get(deviceCode);
        if (connection != null && connection.session().isOpen()) {
            connection.session().send(Mono.just(connection.session().textMessage(message))).subscribe();
        }
    }
}