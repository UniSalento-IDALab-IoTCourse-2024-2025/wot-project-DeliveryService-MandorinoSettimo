package it.unisalento.pas2425.deliveryserviceproject.repositories;

import it.unisalento.pas2425.deliveryserviceproject.domain.Node;
import it.unisalento.pas2425.deliveryserviceproject.domain.NodeType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface NodeRepository extends MongoRepository<Node, String> {
    void deleteById(String id);
    boolean existsById(String id);
    Optional<Node> findFirstByType(NodeType type);

}
