package it.unisalento.pas2425.deliveryserviceproject.components;

import it.unisalento.pas2425.deliveryserviceproject.dto.OptimizeRequestDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.OptimizerResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;


@Component
public class OptimizerClient {

    @Autowired
    private  WebClient webClient;

    public OptimizerResultDTO callPythonOptimizer(OptimizeRequestDTO dto) {

        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(dto);
            System.out.println("📦 JSON inviato a Python: " + json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return webClient.post()
                .uri("/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(OptimizerResultDTO.class)
                .block(); // puoi rendere async se vuoi
    }

    /*public AnomalyOptimizerResultDTO callOptimizeAnomaly(AnomalyOptimizeRequestDTO dto) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(dto);
            System.out.println("📦 JSON inviato a Python (anomaly): " + json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return webClient.post()
                .uri("/optimize-anomaly")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(AnomalyOptimizerResultDTO.class)
                .block(); // o async se preferite
    }*/

}
