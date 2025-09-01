package it.unisalento.pas2425.deliveryserviceproject.components;

import it.unisalento.pas2425.deliveryserviceproject.configuration.GraphhopperConfig;
import it.unisalento.pas2425.deliveryserviceproject.graphhopperutils.GraphhopperPath;
import it.unisalento.pas2425.deliveryserviceproject.graphhopperutils.GraphhopperResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Component
public class GraphhopperClient {
    private final WebClient client;

    public GraphhopperClient(WebClient.Builder builder, GraphhopperConfig config) {
        this.client = builder.baseUrl(config.getGraphhopperUrl()).build();
    }

    private double distance;
    private double time;

    public List<List<Double>> getRoute(double fromLat, double fromLon, double toLat, double toLon) {
        String url = String.format("/route?point=%f,%f&point=%f,%f&profile=car&locale=it&points_encoded=false",
                fromLat, fromLon, toLat, toLon);

        GraphhopperResponse response = client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(GraphhopperResponse.class)
                .block();

        if (response == null || response.getPaths().isEmpty()) return Collections.emptyList();

        GraphhopperPath path = response.getPaths().get(0);
        this.distance = path.getDistance();
        this.time = path.getTime() / 1000.0; // ms → sec

        return path.getPoints().getCoordinates();
    }

    public double getDistance() {
        return distance;
    }

    public double getTime() {
        return time;
    }
}
