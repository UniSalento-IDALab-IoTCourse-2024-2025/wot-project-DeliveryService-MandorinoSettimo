package it.unisalento.pas2425.deliveryserviceproject.dto;

public class RouteResultDTO {
    public static final int OK = 200;
    public static final int ERROR = 500;
    public static final int INVALID_REQUEST = 400;
    public static final int VEHICLE_NOT_MODIFIED = 401;
    public static final int VEHICLE_NOT_FOUND = 402;
    public static final int ROUTE_NOT_FOUND = 403;
    public static final int USER_UPDATE_ERROR = 404;
    public static final int DEPOT_NOT_FOUND = 405;
    public static final int PERSISTENCE_ERROR = 406;
    public static final int GRAPH_ERROR = 406;
    public static final int NO_SOLUTION = 407;



    private int code;
    private String message;
    private OptimizerResultDTO data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OptimizerResultDTO getData() {
        return data;
    }

    public void setData(OptimizerResultDTO data) {
        this.data = data;
    }
}

