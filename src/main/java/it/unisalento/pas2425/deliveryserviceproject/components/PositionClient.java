package it.unisalento.pas2425.deliveryserviceproject.components;

import it.unisalento.pas2425.deliveryserviceproject.configuration.PositionServiceConfig;
import it.unisalento.pas2425.deliveryserviceproject.domain.UserStatus;
import it.unisalento.pas2425.deliveryserviceproject.dto.AssignResultDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.PushTokenResponseDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.UserDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.UserResultDTO;
import it.unisalento.pas2425.deliveryserviceproject.security.JwtUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PositionClient {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private PositionServiceConfig config;

    @Autowired
    private JwtUtilities jwtUtilities;

    public UserResultDTO updateUserStatus(String userId, UserStatus newStatus, String jwtToken) {
        String url = config.getServiceUrl() + "/users/updateStatus/" + userId + "?status=" + newStatus.name();

        try {
            WebClient webClient = webClientBuilder.build();

            if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7);
            }

            return webClient
                    .put()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(UserResultDTO.class)
                    .block(); // blocking per semplicità

        } catch (Exception e) {
            UserResultDTO fallback = new UserResultDTO();
            fallback.setCode(UserResultDTO.ERROR);
            fallback.setMessage("Errore nella chiamata a POSITION_SERVICE: " + e.getMessage());
            return fallback;
        }
    }

    public int countAvailableUsers(String jwtToken) {
        String url = config.getServiceUrl() + "/users/available/count";

        try {
            WebClient webClient = webClientBuilder.build();

            if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7);
            }

            Integer count = webClient
                    .get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .retrieve()
                    .bodyToMono(Integer.class)
                    .block(); // blocking per semplicità

            return count != null ? count : 0;
        } catch (Exception e) {
            System.err.println("Errore nel conteggio utenti disponibili: " + e.getMessage());
            return 0;
        }
    }
    public String getPushTokenByUserId(String jwtToken) {
        if (jwtToken.startsWith("Bearer ")) {
            jwtToken = jwtToken.substring(7);
        }
        String userId = jwtUtilities.extractUserId(jwtToken);
        String url = config.getServiceUrl() + "/users/" + userId + "/push-token";

        try {
            WebClient webClient = webClientBuilder.build();

            if (jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7);
            }

            PushTokenResponseDTO response = webClient
                    .get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .retrieve()
                    .bodyToMono(PushTokenResponseDTO.class)
                    .block();

            if (response != null && response.getCode() == PushTokenResponseDTO.OK) {
                return response.getPushToken();
            } else {
                throw new RuntimeException("Errore nel recupero del token: " + (response != null ? response.getMessage() : "null"));
            }

        } catch (Exception e) {
            System.err.println("Errore durante il recupero del pushToken: " + e.getMessage());
            return null;
        }
    }

    public UserDTO getUserById(String userId, String jwtToken) {
        String url = config.getServiceUrl() + "/users/" + userId;

        try {
            WebClient webClient = webClientBuilder.build();

            if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7);
            }

            return webClient
                    .get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .retrieve()
                    .bodyToMono(UserDTO.class)
                    .block(); // Bloccante per semplicità (va bene nel tuo flusso attuale)
        } catch (Exception e) {
            System.err.println("❌ Errore nel recupero dell'utente " + userId + ": " + e.getMessage());
            throw new RuntimeException("Errore nel recupero dell'utente: " + e.getMessage());
        }
    }


}

