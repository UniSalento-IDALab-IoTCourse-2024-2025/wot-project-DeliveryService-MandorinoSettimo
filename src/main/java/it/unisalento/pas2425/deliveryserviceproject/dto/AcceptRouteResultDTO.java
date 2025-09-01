package it.unisalento.pas2425.deliveryserviceproject.dto;

public class AcceptRouteResultDTO {
    public static final int OK = 200;
    public static final int ROUTE_NOT_FOUND = 401;
    public static final int VEHICLE_NOT_FOUND = 402;
    public static final int VEHICLE_NO_USER_ASSIGNED = 403;
    public static final int USER_UPDATE_FAILED = 404;
    public static final int USER_FETCH_FAILED = 405;
    public static final int ADMIN_TOKEN_NOT_FOUND = 406;
    public static final int NOTIFICATION_FAILED = 407;

    private int code;
    private String message;

    public AcceptRouteResultDTO(int code, String message) {
        this.code = code;
        this.message = message;
    }

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
}
