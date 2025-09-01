package it.unisalento.pas2425.deliveryserviceproject.service;

import it.unisalento.pas2425.deliveryserviceproject.domain.Node;
import it.unisalento.pas2425.deliveryserviceproject.dto.NodeDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.NodeListDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.NodeResultDTO;
import it.unisalento.pas2425.deliveryserviceproject.repositories.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NodeService {

    @Autowired
    private NodeRepository nodeRepository;

    /**
     * Recupera tutti i nodi.
     */
    public NodeListDTO getAllNodes() {
        List<NodeDTO> nodes = nodeRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
        NodeListDTO list = new NodeListDTO();
        list.setNodes(nodes);
        return list;
    }

    /**
     * Recupera un nodo per ID.
     */
    public NodeResultDTO getNodeById(String id) {
        NodeResultDTO result = new NodeResultDTO();
        Optional<Node> optional = nodeRepository.findById(id);

        if (optional.isEmpty()) {
            result.setResult(NodeResultDTO.NODE_NOT_FOUND);
            result.setMessage("Nodo non trovato");
            return result;
        }

        Node node = optional.get();
        result.setResult(NodeResultDTO.OK);
        result.setMessage("Nodo recuperato con successo");
        result.setNode(toDTO(node));

        return result;
    }


    /**
     * Aggiunge un nuovo nodo.
     */
    public NodeResultDTO addNode(NodeDTO dto) {
        NodeResultDTO result = new NodeResultDTO();

        if (dto.getName() == null || dto.getName().isEmpty()) {
            result.setResult(NodeResultDTO.MISSING_DATA);
            result.setMessage("Nome del nodo mancante");
            return result;
        }

        if (dto.getLat() == 0 || dto.getLon() == 0) {
            result.setResult(NodeResultDTO.MISSING_DATA);
            result.setMessage("Latitudine o longitudine mancanti");
            return result;
        }
        if (dto.getId() != null && nodeRepository.findById(dto.getId()).isPresent()) {
            result.setResult(NodeResultDTO.NODE_ALREADY_EXIST);
            result.setMessage("Nodo con lo stesso ID già esistente");
            return result;
        }


        if (dto.getType() == null ) {
            result.setResult(NodeResultDTO.ERROR_TYPE_OF_NODE);
            result.setMessage("Tipo di nodo non valido");
            return result;
        }

        Node node = toEntity(dto);
        node = nodeRepository.save(node);

        result.setResult(NodeResultDTO.OK);
        result.setMessage("Nodo salvato con successo");
        result.setNode(toDTO(node));
        return result;
    }

    /**
     * Aggiorna un nodo esistente.
     */
    public NodeResultDTO updateNode(String id, NodeDTO dto) {
        NodeResultDTO result = new NodeResultDTO();
        Optional<Node> optional = nodeRepository.findById(id);

        if (optional.isEmpty()) {
            result.setResult(NodeResultDTO.NODE_NOT_FOUND);
            result.setMessage("Nodo non trovato");
            return result;
        }

        Node node = optional.get();
        if (dto.getName() != null) node.setName(dto.getName());
        if (dto.getLat() != 0.0) node.setLat(dto.getLat()); // Attenzione ai double!
        if (dto.getLon() != 0.0) node.setLon(dto.getLon()); // Attenzione ai double!
        if (dto.getType() != null) node.setType(dto.getType());
        if (dto.getAddress() != null) node.setAddress(dto.getAddress());

        node = nodeRepository.save(node);

        result.setResult(NodeResultDTO.OK);
        result.setMessage("Nodo aggiornato con successo");
        result.setNode(toDTO(node));
        return result;
    }

    /**
     * Elimina un nodo per ID.
     */
    public NodeResultDTO deleteNode(String id) {
        NodeResultDTO result = new NodeResultDTO();

        if (!nodeRepository.existsById(id)) {
            result.setResult(NodeResultDTO.NODE_NOT_FOUND);
            result.setMessage("Nodo non trovato");
            return result;
        }

        nodeRepository.deleteById(id);
        result.setResult(NodeResultDTO.OK);
        result.setMessage("Nodo eliminato con successo");
        return result;
    }

    public NodeDTO toDTO(Node node) {
        NodeDTO dto = new NodeDTO();
        dto.setId(node.getId());
        dto.setName(node.getName());
        dto.setLat(node.getLat());
        dto.setLon(node.getLon());
        dto.setType(node.getType());
        dto.setAddress(node.getAddress());
        return dto;
    }

    private Node toEntity(NodeDTO dto) {
        Node node = new Node();
        node.setId(dto.getId());
        node.setName(dto.getName());
        node.setLat(dto.getLat());
        node.setLon(dto.getLon());
        node.setType(dto.getType());
        node.setAddress(dto.getAddress());
        return node;
    }
}