package it.unisalento.pas2425.deliveryserviceproject.components;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MqttPublisher {
    private final IMqttClient client;

    public MqttPublisher(
            @Value("${mqtt.client.id:delivery-service}") String clientId,
            @Value("${mqtt.broker.url}") String brokerUrl
    ) throws MqttException {
        // clientId univoco per sicurezza
        this.client = new MqttClient(brokerUrl, clientId + "-" + java.util.UUID.randomUUID());
    }

    @PostConstruct
    public void init() {
        new Thread(() -> {
            int attempt = 0;
            while (!client.isConnected()) {
                try {
                    var opts = new MqttConnectOptions();
                    opts.setAutomaticReconnect(true);
                    opts.setCleanSession(true);
                    client.connect(opts);
                } catch (MqttException e) {
                    attempt++;
                    try { Thread.sleep(Math.min(30000, 1000L * attempt)); } catch (InterruptedException ignored) {}
                }
            }
        }, "mqtt-reconnect").start();
    }

    @PreDestroy
    public void shutdown() {
        try { if (client.isConnected()) client.disconnect(); } catch (Exception ignored) {}
        try { client.close(); } catch (Exception ignored) {}
    }

    public void publishRouteAssigned(String vehicleId, String routeId) {
        try {
            if (!client.isConnected()) return;
            var payload = """
        {"routeId":"%s","ts":"%s"}
      """.formatted(routeId, java.time.Instant.now());
            var msg = new MqttMessage(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            msg.setQos(1);
            msg.setRetained(true);
            client.publish("vehicle/" + vehicleId + "/route-assigned", msg);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void publishRouteStarted(String vehicleId, String routeId, String kind) {
        try {
            if (!client.isConnected()) return;
            String k = "rescue".equalsIgnoreCase(kind) ? "rescue" : "normal";
            String payload = String.format(
                    "{\"routeId\":\"%s\",\"kind\":\"%s\",\"ts\":\"%s\"}",
                    routeId, k, java.time.Instant.now()
            );
            var msg = new MqttMessage(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            msg.setQos(1);
            msg.setRetained(true);
            client.publish("vehicle/" + vehicleId + "/route-started", msg);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void clearRouteStarted(String vehicleId) {
        try {
            if (!client.isConnected()) return;
            var msg = new MqttMessage(new byte[0]);
            msg.setQos(1);
            msg.setRetained(true);
            client.publish("vehicle/" + vehicleId + "/route-started", msg);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
