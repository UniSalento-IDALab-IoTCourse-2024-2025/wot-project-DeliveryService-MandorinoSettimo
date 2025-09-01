package it.unisalento.pas2425.deliveryserviceproject.graphhopperutils;

public class GraphhopperPath {
    private double distance; // in metri
    private long time;       // in millisecondi
    private GraphhopperPoints points;

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public GraphhopperPoints getPoints() {
        return points;
    }

    public void setPoints(GraphhopperPoints points) {
        this.points = points;
    }
}
