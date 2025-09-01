package it.unisalento.pas2425.deliveryserviceproject.dto;

import lombok.Data;

@Data
public class PushTokenResponseDTO {
    public static final int OK = 200;
    public static final int ERROR = 401;

    private int code;
    private String message;
    private String pushToken;

}
