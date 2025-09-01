package it.unisalento.pas2425.deliveryserviceproject.dto;

public class RescueAssignmentDTO {
    private String vehicleId;
    private String orderId;
    private String mode; // PICKED_UP_RESCUE | IN_PROGRESS_REASSIGN
    private double pickupLat, pickupLon;
    private double deliveryLat, deliveryLon;

    public RescueAssignmentDTO(String vehicleId, String orderId, String mode, double pickupLat, double pickupLon, double deliveryLat, double deliveryLon) {
        this.vehicleId = vehicleId;
        this.orderId = orderId;
        this.mode = mode;
        this.pickupLat = pickupLat;
        this.pickupLon = pickupLon;
        this.deliveryLat = deliveryLat;
        this.deliveryLon = deliveryLon;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public double getPickupLat() {
        return pickupLat;
    }

    public void setPickupLat(double pickupLat) {
        this.pickupLat = pickupLat;
    }

    public double getPickupLon() {
        return pickupLon;
    }

    public void setPickupLon(double pickupLon) {
        this.pickupLon = pickupLon;
    }

    public double getDeliveryLat() {
        return deliveryLat;
    }

    public void setDeliveryLat(double deliveryLat) {
        this.deliveryLat = deliveryLat;
    }

    public double getDeliveryLon() {
        return deliveryLon;
    }

    public void setDeliveryLon(double deliveryLon) {
        this.deliveryLon = deliveryLon;
    }
}
