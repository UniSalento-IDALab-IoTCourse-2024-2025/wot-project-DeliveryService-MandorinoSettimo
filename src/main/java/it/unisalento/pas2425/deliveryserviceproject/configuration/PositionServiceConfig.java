package it.unisalento.pas2425.deliveryserviceproject.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PositionServiceConfig {

    @Value("${position.service.url}")
    private String serviceUrl;

    @Value("${position.service.name}")
    private String serviceName;


    public String getServiceUrl() {
        return serviceUrl;
    }

    public String getServiceName() {
        return serviceName;
    }
}
