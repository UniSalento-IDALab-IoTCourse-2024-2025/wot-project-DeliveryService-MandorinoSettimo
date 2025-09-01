package it.unisalento.pas2425.deliveryserviceproject.dto;

import it.unisalento.pas2425.deliveryserviceproject.domain.NodeType;

public class NodeDTO {
    private String id;
    private String name;
    private String address;
    private double lat;
    private double lon;
    private NodeType type;

    public NodeDTO() {
    }

    public NodeDTO(String id, String name, String address, double lat, double lon, NodeType type) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lon = lon;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
