package it.unisalento.pas2425.deliveryserviceproject.dto;

import it.unisalento.pas2425.deliveryserviceproject.domain.OrderStatus;
import it.unisalento.pas2425.deliveryserviceproject.domain.TimeWindow;

public class OrderDTO {
    private String id;
    private String pickupNodeId;
    private String deliveryNodeId;
    private int quantity;
    private int twOpen;
    private int twClose;
    private TimeWindow timeWindow;
    private OrderStatus status;
    private String assignedVehicleId;
    private String vehicleRouteId;

    public OrderDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPickupNodeId() {
        return pickupNodeId;
    }

    public void setPickupNodeId(String pickupNodeId) {
        this.pickupNodeId = pickupNodeId;
    }

    public String getDeliveryNodeId() {
        return deliveryNodeId;
    }

    public void setDeliveryNodeId(String deliveryNodeId) {
        this.deliveryNodeId = deliveryNodeId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getTwOpen() {
        return twOpen;
    }

    public void setTwOpen(int twOpen) {
        this.twOpen = twOpen;
    }

    public int getTwClose() {
        return twClose;
    }

    public TimeWindow getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(TimeWindow timeWindow) {
        this.timeWindow = timeWindow;
    }

    public void setTwClose(int twClose) {
        this.twClose = twClose;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getAssignedVehicleId() {
        return assignedVehicleId;
    }

    public void setAssignedVehicleId(String assignedVehicleId) {
        this.assignedVehicleId = assignedVehicleId;
    }

    public String getVehicleRouteId() {
        return vehicleRouteId;
    }

    public void setVehicleRouteId(String vehicleRouteId) {
        this.vehicleRouteId = vehicleRouteId;
    }
}
