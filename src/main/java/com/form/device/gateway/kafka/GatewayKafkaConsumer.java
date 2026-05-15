package com.form.device.gateway.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.form.device.gateway.dto.DeviceMessage;
import com.form.device.gateway.dto.KafkaMessage;
import com.form.device.gateway.service.DeviceSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final DeviceSessionManager sessionManager;

    @KafkaListener(topics = "print-jobs", groupId = "print-jobs-group")
    public void printJobsConsume(ConsumerRecord<String, String> record) {
        try {
            KafkaMessage kafkaMessage = objectMapper.readValue(record.value(), KafkaMessage.class);
            String deviceCode = kafkaMessage.deviceCode();
            String message    = kafkaMessage.data();
            sessionManager.send(deviceCode, objectMapper.writeValueAsString(new DeviceMessage("print",message)));
        } catch (JsonProcessingException e) {
            logMessage(e);
        }
    }

    private static void logMessage(JsonProcessingException e) {
        log.debug("print-jobs error :: "+ e.getMessage());
    }

    @KafkaListener(topics = "jobs-payment", groupId = "jobs-payment-group")
    public void jobsPaymentConsume(ConsumerRecord<String, String> record) {
        try {
            KafkaMessage kafkaMessage = objectMapper.readValue(record.value(), KafkaMessage.class);
            String deviceCode = kafkaMessage.deviceCode();
            String message    = kafkaMessage.data();
            sessionManager.send(deviceCode, objectMapper.writeValueAsString(new DeviceMessage("payment",message)));
        } catch (JsonProcessingException e) {
            logMessage(e);
        }
    }

    @KafkaListener(topics = "scan-jobs", groupId = "scan-jobs-group")
    public void scanJobsConsume(ConsumerRecord<String, String> record) {
        try {
            KafkaMessage kafkaMessage = objectMapper.readValue(record.value(), KafkaMessage.class);
            String deviceCode = kafkaMessage.deviceCode();
            String message    = kafkaMessage.data();
            sessionManager.send(deviceCode, objectMapper.writeValueAsString(new DeviceMessage("scan",message)));
        } catch (JsonProcessingException e) {
            logMessage(e);
        }
    }

    @KafkaListener(topics = "scan-upload-jobs", groupId = "scan-upload-jobs-group")
    public void scanUploadJobsConsume(ConsumerRecord<String, String> record) {
        try {
            KafkaMessage kafkaMessage = objectMapper.readValue(record.value(), KafkaMessage.class);
            String deviceCode = kafkaMessage.deviceCode();
            String message    = kafkaMessage.data();
            sessionManager.send(deviceCode, objectMapper.writeValueAsString(new DeviceMessage("scanUpload",message)));
        } catch (JsonProcessingException e) {
            logMessage(e);
        }
    }
}
