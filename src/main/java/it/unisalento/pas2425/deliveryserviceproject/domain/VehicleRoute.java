package it.unisalento.pas2425.deliveryserviceproject.domain;

import it.unisalento.pas2425.deliveryserviceproject.dto.RouteNodeDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("vehicle_routes")
public class VehicleRoute {
    @Id
    private String id;
    private String vehicleId;
    private List<RouteNodeDTO> route;
    private VehicleStatus vehicleStatus;
    private boolean completed;
    private String associatedUserId;
    private int currentSegmentIndex; // indice della tappa corrente

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public List<RouteNodeDTO> getRoute() {
        return route;
    }

    public void setRoute(List<RouteNodeDTO> route) {
        this.route = route;
    }

    public VehicleStatus getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(VehicleStatus vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getAssociatedUserId() {
        return associatedUserId;
    }

    public void setAssociatedUserId(String associatedUserId) {
        this.associatedUserId = associatedUserId;
    }

    public int getCurrentSegmentIndex() {
        return currentSegmentIndex;
    }

    public void setCurrentSegmentIndex(int currentSegmentIndex) {
        this.currentSegmentIndex = currentSegmentIndex;
    }
}
