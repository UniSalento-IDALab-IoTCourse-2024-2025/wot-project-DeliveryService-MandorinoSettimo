package it.unisalento.pas2425.deliveryserviceproject.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unisalento.pas2425.deliveryserviceproject.domain.Vehicle;
import it.unisalento.pas2425.deliveryserviceproject.dto.PositionDTO;
import it.unisalento.pas2425.deliveryserviceproject.mapper.GeoUtils;
import it.unisalento.pas2425.deliveryserviceproject.repositories.VehicleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class MqttSubscriber {

    private final VehicleRepository vehicleRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final IMqttClient client;

    // prende da application.properties: mqtt.broker.url=${MQTT_BROKER_URL:tcp://mosquitto:1883}
    public MqttSubscriber(
            VehicleRepository vehicleRepository,
            @Value("${mqtt.broker.url}") String brokerUrl
    ) throws MqttException {
        this.vehicleRepository = vehicleRepository;
        String clientId = "delivery-service-listener-" + UUID.randomUUID();
        this.client = new MqttClient(brokerUrl, clientId);
    }

    @PostConstruct
    public void init() {
        // connect con retry/backoff
        new Thread(() -> {
            int attempt = 0;
            while (!client.isConnected()) {
                try {
                    MqttConnectOptions opts = new MqttConnectOptions();
                    opts.setAutomaticReconnect(true);
                    opts.setCleanSession(true);
                    client.connect(opts);
                } catch (Exception e) {
                    attempt++;
                    try { Thread.sleep(Math.min(30000, 1000L * attempt)); } catch (InterruptedException ignored) {}
                }
            }
            // una volta connesso → subscribe
            try {
                client.subscribe("vehicle/+/position", (topic, msg) -> handlePosition(topic, msg));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "mqtt-subscriber-reconnect").start();
    }

    private void handlePosition(String topic, MqttMessage msg) {
        try {
            String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
            PositionDTO pos = mapper.readValue(payload, PositionDTO.class);

            // topic: vehicle/{vehicleId}/position
            String[] parts = topic.split("/");
            String vehicleId = parts.length > 1 ? parts[1] : null;

            if (vehicleId != null) {
                vehicleRepository.findById(vehicleId).ifPresent(v -> {
                    // eventuali valori dal client (pos hanno questi campi opzionali)
                    Double speedFromClient = pos.getSpeedKmh();   // può essere null
                    Double headingFromClient = pos.getHeading();  // può essere null

                    // fallback server: calcolo se mancano e ho una posizione precedente
                    if ((speedFromClient == null || headingFromClient == null)
                            && v.getCurrentLat()!=null && v.getCurrentLon()!=null && v.getLastPositionAt()!=null) {

                        // protezione anti-spike: calcola solo se deltaT >= 1s
                        String t1 = v.getLastPositionAt().toString();
                        String t2 = pos.getTimestamp();
                        if (t2 != null) {
                            long dtMs = java.time.Instant.parse(t2).toEpochMilli() -
                                    java.time.Instant.parse(t1).toEpochMilli();
                            if (dtMs >= 1000) {
                                if (speedFromClient == null) {
                                    speedFromClient = GeoUtils.speedKmh(
                                            t1, v.getCurrentLat(), v.getCurrentLon(),
                                            t2, pos.getLat(), pos.getLon()
                                    );
                                }
                                if (headingFromClient == null) {
                                    headingFromClient = GeoUtils.bearingDegrees(
                                            v.getCurrentLat(), v.getCurrentLon(),
                                            pos.getLat(), pos.getLon()
                                    );
                                }
                            }
                        }
                    }

                    v.setLastSpeedKmh(speedFromClient);
                    v.setLastHeadingDeg(headingFromClient);

                    v.setCurrentLat(pos.getLat());
                    v.setCurrentLon(pos.getLon());
                    if (pos.getTimestamp() != null) {
                        v.setLastPositionAt(java.time.Instant.parse(pos.getTimestamp()));
                    }
                    vehicleRepository.save(v);
                });

            }

            System.out.println("[" + Instant.now() + "] MQTT position " + topic + " -> " + payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void shutdown() {
        try { if (client.isConnected()) client.disconnect(); } catch (Exception ignored) {}
        try { client.close(); } catch (Exception ignored) {}
    }
}