package it.unisalento.pas2425.deliveryserviceproject.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("vehicles")
public class Vehicle {
    @Id
    private String id;
    private String plate;
    private int capacity;
    private int cost;
    private Double currentLat;
    private Double currentLon;
    private VehicleStatus status;
    private String assignedUserId;
    private Double lastSpeedKmh;
    private Double lastHeadingDeg;
    private Instant lastPositionAt;

    public Vehicle() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public Double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(Double currentLat) {
        this.currentLat = currentLat;
    }

    public Double getCurrentLon() {
        return currentLon;
    }

    public void setCurrentLon(Double currentLon) {
        this.currentLon = currentLon;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public String getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(String assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public Double getLastSpeedKmh() {
        return lastSpeedKmh;
    }

    public void setLastSpeedKmh(Double lastSpeedKmh) {
        this.lastSpeedKmh = lastSpeedKmh;
    }

    public Double getLastHeadingDeg() {
        return lastHeadingDeg;
    }

    public void setLastHeadingDeg(Double lastHeadingDeg) {
        this.lastHeadingDeg = lastHeadingDeg;
    }

    public Instant getLastPositionAt() {
        return lastPositionAt;
    }

    public void setLastPositionAt(Instant lastPositionAt) {
        this.lastPositionAt = lastPositionAt;
    }
}
