package it.unisalento.pas2425.deliveryserviceproject.repositories;

import it.unisalento.pas2425.deliveryserviceproject.domain.VehicleRoute;
import it.unisalento.pas2425.deliveryserviceproject.domain.VehicleStatus;
import it.unisalento.pas2425.deliveryserviceproject.dto.VehicleRouteDTO;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRouteRepository extends MongoRepository<VehicleRoute, String> {
    List<VehicleRoute> findAllByVehicleIdAndCompletedFalse(String vehicleId);
    Optional<VehicleRouteDTO> findByVehicleId(String vehicleId);
    List<VehicleRoute> findAllByAssociatedUserId(String userId);
    Optional<VehicleRoute> findByAssociatedUserIdAndVehicleStatus(String associatedUserId, VehicleStatus status);
    Optional<VehicleRoute> findFirstByVehicleIdAndCompletedFalseOrderByIdDesc(String vehicleId);

    // This interface can be extended with custom query methods if needed
    // For example, to find routes by vehicle ID or other criteria
}
