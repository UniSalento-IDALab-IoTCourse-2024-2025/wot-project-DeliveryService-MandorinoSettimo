package it.unisalento.pas2425.deliveryserviceproject.restcontroller;

import it.unisalento.pas2425.deliveryserviceproject.dto.NodeDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.NodeListDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.NodeResultDTO;
import it.unisalento.pas2425.deliveryserviceproject.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nodes")
public class NodeRestController {

    @Autowired
    private NodeService nodeService;

    @GetMapping
    public ResponseEntity<NodeListDTO> getAllNodes() {
        NodeListDTO nodes = nodeService.getAllNodes();
        return ResponseEntity.ok(nodes);
    }

    @PostMapping
    public ResponseEntity<NodeResultDTO> createNode(@RequestBody NodeDTO dto) {
        NodeResultDTO createdNode = nodeService.addNode(dto);
        return createdNode.getResult() == NodeResultDTO.OK
                ? ResponseEntity.ok(createdNode)
                : ResponseEntity.badRequest().body(createdNode);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NodeResultDTO> getNodeById(@PathVariable String id) {
        NodeResultDTO node = nodeService.getNodeById(id);
        return node.getResult() == NodeResultDTO.OK
                ? ResponseEntity.ok(node)
                : ResponseEntity.badRequest().body(node);
    }



    @PutMapping("/{id}")
    public ResponseEntity<NodeResultDTO> updateNode(@PathVariable String id, @RequestBody NodeDTO dto) {
        NodeResultDTO updatedNode = nodeService.updateNode(id, dto);
        return updatedNode.getResult() == NodeResultDTO.OK
                ? ResponseEntity.ok(updatedNode)
                : ResponseEntity.badRequest().body(updatedNode);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<NodeResultDTO> deleteNode(@PathVariable String id) {
        NodeResultDTO result = nodeService.deleteNode(id);
        return result.getResult() == NodeResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }


}

