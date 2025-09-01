package it.unisalento.pas2425.deliveryserviceproject.graphhopperutils;

import java.util.List;

public class GraphhopperResponse {
    private List<GraphhopperPath> paths;

    public List<GraphhopperPath> getPaths() {
        return paths;
    }

    public void setPaths(List<GraphhopperPath> paths) {
        this.paths = paths;
    }
}
