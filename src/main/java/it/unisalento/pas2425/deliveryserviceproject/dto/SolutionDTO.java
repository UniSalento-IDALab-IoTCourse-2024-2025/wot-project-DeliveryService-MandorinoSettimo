package it.unisalento.pas2425.deliveryserviceproject.dto;

import java.util.List;

public class SolutionDTO {
    private List<VehicleRouteDTO> path;
    private List<AssignedOrderDTO> assignedOrders;

    public List<VehicleRouteDTO> getPath() {
        return path;
    }

    public void setPath(List<VehicleRouteDTO> path) {
        this.path = path;
    }

    public List<AssignedOrderDTO> getAssignedOrders() {
        return assignedOrders;
    }

    public void setAssignedOrders(List<AssignedOrderDTO> assignedOrders) {
        this.assignedOrders = assignedOrders;
    }
}
