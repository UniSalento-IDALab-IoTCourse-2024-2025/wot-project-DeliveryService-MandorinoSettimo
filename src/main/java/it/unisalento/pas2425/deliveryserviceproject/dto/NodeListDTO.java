package it.unisalento.pas2425.deliveryserviceproject.dto;

import java.util.List;

public class NodeListDTO {
    private List<NodeDTO> nodes;

    public NodeListDTO() {}

    public NodeListDTO(List<NodeDTO> nodes) {
        this.nodes = nodes;
    }

    public List<NodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeDTO> nodes) {
        this.nodes = nodes;
    }
}
