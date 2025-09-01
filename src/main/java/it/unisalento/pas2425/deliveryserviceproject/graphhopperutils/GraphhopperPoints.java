package it.unisalento.pas2425.deliveryserviceproject.graphhopperutils;

import java.util.List;

public class GraphhopperPoints {
    private String type; // di solito "LineString"
    private List<List<Double>> coordinates; // [ [lon, lat], [lon, lat], ... ]

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<List<Double>> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<List<Double>> coordinates) {
        this.coordinates = coordinates;
    }
}
