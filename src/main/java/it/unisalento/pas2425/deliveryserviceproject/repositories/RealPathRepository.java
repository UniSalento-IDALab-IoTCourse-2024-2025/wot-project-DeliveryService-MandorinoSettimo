package it.unisalento.pas2425.deliveryserviceproject.repositories;

import it.unisalento.pas2425.deliveryserviceproject.domain.RealPath;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RealPathRepository extends MongoRepository<RealPath, String> {
    RealPath findByFromNodeIndexAndToNodeIndex(int fromNodeIndex, int toNodeIndex);
    boolean existsByFromNodeIndexAndToNodeIndex(int fromNodeIndex, int toNodeIndex);
    List<RealPath> findByRouteId(String routeId);
    void deleteByRouteId(String routeId);
}

