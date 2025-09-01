package it.unisalento.pas2425.deliveryserviceproject.components;

import it.unisalento.pas2425.deliveryserviceproject.configuration.NotificationServiceConfig;
import it.unisalento.pas2425.deliveryserviceproject.dto.NotificationRequestDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.NotificationResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class NotificationClient {

    @Autowired
    private NotificationServiceConfig notificationServiceConfig;

    private WebClient getWebClient() {
        return WebClient.builder()
                .baseUrl(notificationServiceConfig.getServiceUrl())
                .build();
    }

    public NotificationResponseDTO sendNotification(NotificationRequestDTO request) {
        return getWebClient().post()
                .uri("/notify/send")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(NotificationResponseDTO.class)
                .block(); // ❗ Usa block() se sei nel flusso sincrono
    }
}
