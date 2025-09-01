package it.unisalento.pas2425.deliveryserviceproject.dto;

import java.time.Instant;
import java.util.List;

public class GreedyInputDTO {
    private List<OrderDTO> allOrders;
    private List<VehicleDTO> vehicles;
    private List<NodeDTO> nodes;
    private String brokenVehicleId;
    private double anomalyLat;
    private double anomalyLon;
    private String depotNodeId;
    private Instant now;

    public GreedyInputDTO(List<OrderDTO> allOrders, List<VehicleDTO> vehicles, List<NodeDTO> nodes, String brokenVehicleId, double anomalyLat, double anomalyLon, String depotNodeId, Instant now) {
        this.allOrders = allOrders;
        this.vehicles = vehicles;
        this.nodes = nodes;
        this.brokenVehicleId = brokenVehicleId;
        this.anomalyLat = anomalyLat;
        this.anomalyLon = anomalyLon;
        this.depotNodeId = depotNodeId;
        this.now = now;
    }

    public List<OrderDTO> getAllOrders() {
        return allOrders;
    }

    public void setAllOrders(List<OrderDTO> allOrders) {
        this.allOrders = allOrders;
    }

    public List<VehicleDTO> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<VehicleDTO> vehicles) {
        this.vehicles = vehicles;
    }

    public List<NodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeDTO> nodes) {
        this.nodes = nodes;
    }

    public String getBrokenVehicleId() {
        return brokenVehicleId;
    }

    public void setBrokenVehicleId(String brokenVehicleId) {
        this.brokenVehicleId = brokenVehicleId;
    }

    public double getAnomalyLat() {
        return anomalyLat;
    }

    public void setAnomalyLat(double anomalyLat) {
        this.anomalyLat = anomalyLat;
    }

    public double getAnomalyLon() {
        return anomalyLon;
    }

    public void setAnomalyLon(double anomalyLon) {
        this.anomalyLon = anomalyLon;
    }

    public String getDepotNodeId() {
        return depotNodeId;
    }

    public void setDepotNodeId(String depotNodeId) {
        this.depotNodeId = depotNodeId;
    }

    public Instant getNow() {
        return now;
    }

    public void setNow(Instant now) {
        this.now = now;
    }
}
