package it.unisalento.pas2425.deliveryserviceproject.dto;

import java.time.Instant;

public class ReportAnomalyRequestDTO {
    private String vehicleId;
    private String userId;
    private String activeRouteId; // utile per recuperare gli ordini
    private Double anomalyLat;     // opzionale: se assente uso currentLat del veicolo
    private Double anomalyLon;
    private Instant timestamp;

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActiveRouteId() {
        return activeRouteId;
    }

    public void setActiveRouteId(String activeRouteId) {
        this.activeRouteId = activeRouteId;
    }

    public Double getAnomalyLat() {
        return anomalyLat;
    }

    public void setAnomalyLat(Double anomalyLat) {
        this.anomalyLat = anomalyLat;
    }

    public Double getAnomalyLon() {
        return anomalyLon;
    }

    public void setAnomalyLon(Double anomalyLon) {
        this.anomalyLon = anomalyLon;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
