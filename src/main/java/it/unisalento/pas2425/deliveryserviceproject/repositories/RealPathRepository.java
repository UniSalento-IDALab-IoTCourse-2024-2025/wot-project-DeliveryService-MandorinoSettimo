package it.unisalento.pas2425.deliveryserviceproject.repositories;

import it.unisalento.pas2425.deliveryserviceproject.domain.RealPath;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RealPathRepository extends MongoRepository<RealPath, String> {
    List<RealPath> findByRouteId(String routeId);
    void deleteByRouteId(String routeId);

    Optional<RealPath> findFirstByRouteIdAndFromNodeIndexAndToNodeIndex(
            String routeId, String fromNodeIndex, String toNodeIndex
    );
}

