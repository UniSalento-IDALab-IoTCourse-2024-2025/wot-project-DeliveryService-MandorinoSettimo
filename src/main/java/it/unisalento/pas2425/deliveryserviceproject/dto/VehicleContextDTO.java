package it.unisalento.pas2425.deliveryserviceproject.dto;

@lombok.Data @lombok.Builder
public class VehicleContextDTO {
    private String vehicleId;
    private String routeId;
    private Integer currentSegmentIndex;

    private String currentSegmentId;


    @lombok.Data @lombok.AllArgsConstructor
    public static class StopName { private String name; }

    @lombok.Data @lombok.AllArgsConstructor
    public static class NextStop { private String name; private Integer etaMinutes; }

    @lombok.Data @lombok.AllArgsConstructor
    public static class OrderLite { private String orderId; private String type; private String pickupFrom; private String deliveryTo; }

    @lombok.Data @lombok.AllArgsConstructor
    public static class Point { private double lat; private double lon; }

    private NextStop nextStop;
    private java.util.List<StopName> upcomingStops;
    private java.util.List<OrderLite> ordersInProgress;
    private java.util.List<Point> currentSegmentPolyline; // opzionale
}
