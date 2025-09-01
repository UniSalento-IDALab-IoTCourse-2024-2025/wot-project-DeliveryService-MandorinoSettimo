package it.unisalento.pas2425.deliveryserviceproject.dto;

import java.util.List;

public class RealPathDTO {
    private String id;
    private String fromNodeIndex;
    private String toNodeIndex;
    private String fromLabel;
    private String toLabel;
    private List<List<Double>> geometry; // [ [lon, lat], [lon, lat], ... ]
    private double distanceM;
    private double timeS;
    private String vehicleId;
    private String routeId;
    private List<String> orderIds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromNodeIndex() {
        return fromNodeIndex;
    }

    public void setFromNodeIndex(String fromNodeIndex) {
        this.fromNodeIndex = fromNodeIndex;
    }

    public String getToNodeIndex() {
        return toNodeIndex;
    }

    public void setToNodeIndex(String toNodeIndex) {
        this.toNodeIndex = toNodeIndex;
    }

    public String getFromLabel() {
        return fromLabel;
    }

    public void setFromLabel(String fromLabel) {
        this.fromLabel = fromLabel;
    }

    public String getToLabel() {
        return toLabel;
    }

    public void setToLabel(String toLabel) {
        this.toLabel = toLabel;
    }

    public List<List<Double>> getGeometry() {
        return geometry;
    }

    public void setGeometry(List<List<Double>> geometry) {
        this.geometry = geometry;
    }

    public double getDistanceM() {
        return distanceM;
    }

    public void setDistanceM(double distanceM) {
        this.distanceM = distanceM;
    }

    public double getTimeS() {
        return timeS;
    }

    public void setTimeS(double timeS) {
        this.timeS = timeS;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public List<String> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<String> orderIds) {
        this.orderIds = orderIds;
    }
}
