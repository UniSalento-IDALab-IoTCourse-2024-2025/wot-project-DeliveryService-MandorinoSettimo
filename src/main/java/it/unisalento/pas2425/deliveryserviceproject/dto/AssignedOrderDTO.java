package it.unisalento.pas2425.deliveryserviceproject.dto;

public class AssignedOrderDTO {
    private String orderId;
    private String pickupNodeId;
    private String deliveryNodeId;
    private String assignedVehicleId;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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

    public String getAssignedVehicleId() {
        return assignedVehicleId;
    }

    public void setAssignedVehicleId(String assignedVehicleId) {
        this.assignedVehicleId = assignedVehicleId;
    }
}
