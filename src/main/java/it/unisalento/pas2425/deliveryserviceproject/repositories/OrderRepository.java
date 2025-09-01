package it.unisalento.pas2425.deliveryserviceproject.repositories;

import it.unisalento.pas2425.deliveryserviceproject.domain.Order;
import it.unisalento.pas2425.deliveryserviceproject.domain.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    void deleteById(String orderId);
    boolean existsById(String orderId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByVehicleRouteId(String routeId);
    List<Order> findByVehicleRouteIdAndStatus(String vehicleRouteId, OrderStatus status);
    List<Order> findByAssignedVehicleIdAndStatusIn(String assignedVehicleId, Collection<OrderStatus> status);



}
