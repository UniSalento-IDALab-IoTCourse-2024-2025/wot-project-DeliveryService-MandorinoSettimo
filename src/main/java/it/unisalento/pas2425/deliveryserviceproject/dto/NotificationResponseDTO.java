package it.unisalento.pas2425.deliveryserviceproject.dto;

import lombok.Data;

@Data
public class NotificationResponseDTO {
    public static final int OK = 200;

    private int code;
    private String message;

    public NotificationResponseDTO(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
