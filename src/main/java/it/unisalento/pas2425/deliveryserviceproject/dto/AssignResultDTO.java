package it.unisalento.pas2425.deliveryserviceproject.dto;

public class AssignResultDTO {
    public static final int OK = 200;
    public static final int VEHICLE_NOT_FOUND = 401;
    public static final int VEHICLE_NOT_AVAILABLE = 403;
    public static final int VEHICLE_OK_USER_ERROR = 404;
    public static final int USER_UPDATE_FAILED = 405;
    public static final int PUSH_TOKEN_NOT_FOUND = 406;

    private int code;
    private String message;

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
