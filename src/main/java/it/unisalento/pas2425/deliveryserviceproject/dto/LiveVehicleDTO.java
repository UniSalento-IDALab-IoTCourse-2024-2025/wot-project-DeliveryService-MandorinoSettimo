package it.unisalento.pas2425.deliveryserviceproject.dto;

@lombok.Data @lombok.Builder
public class LiveVehicleDTO {
    private String vehicleId;
    private String plate;
    private String status;           // "IN_TRANSIT" | "AVAILABLE"
    private double lat;
    private double lon;
    private String lastUpdate;       // ISO8601
    private boolean isStale;
    private String driverName;       // da assignedUserId -> UserDTO
    private Double speedKmh;         // opzionale
    private Double heading;          // opzionale
}

