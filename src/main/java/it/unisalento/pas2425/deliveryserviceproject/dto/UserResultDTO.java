package it.unisalento.pas2425.deliveryserviceproject.dto;

public class UserResultDTO {
    public static final int OK = 200;
    public static final int USER_NOT_FOUND = 401;
    public static final int USER_NOT_AVAILABLE = 402;
    public static final int ERROR = 500;
    private int code;
    private String message;

    public UserResultDTO(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public UserResultDTO() {
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
