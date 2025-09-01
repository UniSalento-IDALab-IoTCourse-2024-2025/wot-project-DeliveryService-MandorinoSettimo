package it.unisalento.pas2425.deliveryserviceproject.dto;

import java.util.List;

public class OptimizeRequestDTO {
    private List<NodeDTO> nodes;
    private List<OrderDTO> orders;
    private List<VehicleDTO> vehicles;

    public OptimizeRequestDTO() {}

    public List<NodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeDTO> nodes) {
        this.nodes = nodes;
    }

    public List<OrderDTO> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderDTO> orders) {
        this.orders = orders;
    }

    public List<VehicleDTO> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<VehicleDTO> vehicles) {
        this.vehicles = vehicles;
    }
}
