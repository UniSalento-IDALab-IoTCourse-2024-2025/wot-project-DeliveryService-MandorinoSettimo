package it.unisalento.pas2425.deliveryserviceproject.dto;

public class VehicleResultDTO {

    public static final int OK = 200;
    public static final int VEHICLE_NOT_FOUND = 401;
    public static final int MISSING_DATA = 402;
    public static final int VEHICLE_ALREADY_EXIST = 403;
    public static final int ERROR_TYPE_OF_VEHICLE = 404;
    public static final int INVALID_PLATE_FORMAT = 405;
    public static final int VEHICLE_IN_TRANSIT = 406;

    private int result;
    private String message;
    private VehicleDTO vehicle;
    // Getter & Setter

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public VehicleDTO getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleDTO vehicle) {
        this.vehicle = vehicle;
    }

}
