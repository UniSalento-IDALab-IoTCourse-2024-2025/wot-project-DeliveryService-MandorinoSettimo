package it.unisalento.pas2425.deliveryserviceproject.dto;

import it.unisalento.pas2425.deliveryserviceproject.domain.VehicleStatus;

import java.util.List;

public class VehicleRouteDTO {
    private String id;
    private String vehicleId;
    private List<RouteNodeDTO> route;
    private VehicleStatus vehicleStatus;
    private boolean completed;
    private String associatedUserId;
    private int currentSegmentIndex; // indice della tappa corrente
    private List<String> orderIds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public List<RouteNodeDTO> getRoute() {
        return route;
    }

    public void setRoute(List<RouteNodeDTO> route) {
        this.route = route;
    }

    public VehicleStatus getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(VehicleStatus vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getAssociatedUserId() {
        return associatedUserId;
    }

    public void setAssociatedUserId(String associatedUserId) {
        this.associatedUserId = associatedUserId;
    }

    public int getCurrentSegmentIndex() {
        return currentSegmentIndex;
    }

    public void setCurrentSegmentIndex(int currentSegmentIndex) {
        this.currentSegmentIndex = currentSegmentIndex;
    }

    public List<String> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<String> orderIds) {
        this.orderIds = orderIds;
    }
}
