package com.form.device.gateway.dto;

public record KafkaMessage(
        String deviceCode,
        String data
) {
}
