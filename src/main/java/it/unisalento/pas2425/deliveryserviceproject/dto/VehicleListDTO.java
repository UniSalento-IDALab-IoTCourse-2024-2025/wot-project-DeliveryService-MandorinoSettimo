package it.unisalento.pas2425.deliveryserviceproject.dto;

import java.util.List;

public class VehicleListDTO {
    private List<VehicleDTO> vehicles;

    public VehicleListDTO() {}

    public VehicleListDTO(List<VehicleDTO> vehicles) {
        this.vehicles = vehicles;
    }

    public List<VehicleDTO> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<VehicleDTO> vehicles) {
        this.vehicles = vehicles;
    }
}
