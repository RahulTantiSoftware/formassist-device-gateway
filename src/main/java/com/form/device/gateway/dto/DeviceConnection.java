package com.form.device.gateway.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.reactive.socket.WebSocketSession;

@Setter
@Getter
public class DeviceConnection{
     private final WebSocketSession session;
     private long lastHeartbeat;

    public DeviceConnection(WebSocketSession session) {
        this.session = session;
        lastHeartbeat = System.currentTimeMillis();
    }
}
