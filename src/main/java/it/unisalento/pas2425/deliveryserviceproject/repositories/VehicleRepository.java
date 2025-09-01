package it.unisalento.pas2425.deliveryserviceproject.repositories;

import it.unisalento.pas2425.deliveryserviceproject.domain.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    Optional<Vehicle> findByPlate(String plate);
    List<Vehicle> findByStatus(String status);
    List<Vehicle> findByAssignedUserId(String userId);

}
