package it.unisalento.pas2425.deliveryserviceproject.dto;

import lombok.Data;

import java.util.Map;

@Data
public class NotificationRequestDTO {
    private String to;     // ExponentPushToken[...] dell'utente
    private String title;
    private String body;
    private Map<String, Object> data;

}