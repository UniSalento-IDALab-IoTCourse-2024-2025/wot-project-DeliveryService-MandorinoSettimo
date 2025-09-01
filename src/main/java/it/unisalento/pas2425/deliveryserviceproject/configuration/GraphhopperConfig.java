package it.unisalento.pas2425.deliveryserviceproject.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphhopperConfig {

    @Value("${graphhopper.url}")
    private String graphhopperUrl;

    public String getGraphhopperUrl() {
        return graphhopperUrl;
    }
}

