package it.unisalento.pas2425.deliveryserviceproject.service;

import it.unisalento.pas2425.deliveryserviceproject.components.PositionClient;
import it.unisalento.pas2425.deliveryserviceproject.domain.*;
import it.unisalento.pas2425.deliveryserviceproject.dto.LiveVehicleDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.RouteNodeDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.UserDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.VehicleContextDTO;
import it.unisalento.pas2425.deliveryserviceproject.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminLiveService {

    private static final double DEPOT_LAT = 40.3362389; // <-- i tuoi
    private static final double DEPOT_LON = 18.1111071;
    private static final long STALE_SECONDS = 120;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PositionClient positionClient; // per chiamare PositionService (se serve)

    @Autowired
    private VehicleRouteRepository vehicleRouteRepository;

    @Autowired
    private RealPathRepository realPathRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private OrderRepository orderRepository;               // o client a DeliveryService

    /** /admin/vehicles/live */
    public List<LiveVehicleDTO> getLiveVehicles(String token) {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        Instant now = Instant.now();

        return vehicles.stream().map(v -> {
            // Driver name via client (se assegnato)
            String driverName = null;
            if (v.getAssignedUserId() != null) {
                try {
                    UserDTO u = positionClient.getUserById(v.getAssignedUserId(), token);
                    if (u != null) driverName = (u.getName() + " " + u.getSurname()).trim();
                } catch (Exception e) {
                    // silenzioso: non bloccare la lista se fallisce la chiamata esterna
                    System.err.println("⚠️ getUserById failed for " + v.getAssignedUserId() + ": " + e.getMessage());
                }
            }

            boolean isStale = v.getLastPositionAt() == null ||
                    Duration.between(v.getLastPositionAt(), now).getSeconds() > STALE_SECONDS;

            double lat = (v.getCurrentLat() != null) ? v.getCurrentLat() : DEPOT_LAT;
            double lon = (v.getCurrentLon() != null) ? v.getCurrentLon() : DEPOT_LON;

            // opzionale: far vedere gli AVAILABLE al depot
            if (v.getStatus() == VehicleStatus.AVAILABLE) {
                lat = DEPOT_LAT;
                lon = DEPOT_LON;
            }

            return LiveVehicleDTO.builder()
                    .vehicleId(v.getId())
                    .plate(v.getPlate())
                    .status(v.getStatus().name())
                    .lat(lat).lon(lon)
                    .lastUpdate(v.getLastPositionAt() != null ? v.getLastPositionAt().toString() : null)
                    .isStale(isStale)
                    .driverName(driverName)
                    .speedKmh(v.getLastSpeedKmh())
                    .heading(v.getLastHeadingDeg())
                    .build();
        }).toList();
    }

    /** /admin/vehicles/{id}/context */
    public VehicleContextDTO getVehicleContext(String vehicleId) {
        VehicleRoute route = vehicleRouteRepository
                .findFirstByVehicleIdAndCompletedFalseOrderByIdDesc(vehicleId)
                .orElse(null);
        if (route == null) return null;

        Integer idx = route.getCurrentSegmentIndex();
        String routeId = route.getId();

        // Passo 1: calcola nextStop + upcomingStops dai RouteNode (Entity)
        List<RouteNodeDTO> steps = route.getRoute(); // se hai embeddeds o JSON, adattalo al tuo tipo
        String nextName = null;
        List<VehicleContextDTO.StopName> upcoming = new ArrayList<>();

        if (steps != null && idx != null && idx >= 0 && idx < steps.size()-1) {
            String toNodeIndex = steps.get(idx+1).getNodeIndex();
            Node toNode = nodeRepository.findById(toNodeIndex).orElse(null);
            nextName = toNode != null ? toNode.getName() : ("Nodo " + toNodeIndex);

            for (int k = idx+2; k < Math.min(steps.size(), idx+5); k++) {
                String ni = steps.get(k).getNodeIndex();
                Node n = nodeRepository.findById(ni).orElse(null);
                upcoming.add(new VehicleContextDTO.StopName(n != null ? n.getName() : ("Nodo " + ni)));
            }
        }

        // Passo 2: Ordini in corso (Entity -> lite)
        // Se vuoi solo certi stati:
        List<Order> ords = orderRepository.findByVehicleRouteIdAndStatusIn(routeId, List.of(
                OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS, OrderStatus.PICKED_UP
        ));
        List<VehicleContextDTO.OrderLite> orders = new ArrayList<>();
        for (Order o : ords) {
            Node pick = nodeRepository.findById(o.getPickupNodeId()).orElse(null);
            Node del  = nodeRepository.findById(o.getDeliveryNodeId()).orElse(null);
            orders.add(new VehicleContextDTO.OrderLite(
                    o.getId(),
                    o.getStatus() != null ? o.getStatus().name() : "UNKNOWN",
                    pick != null ? pick.getName() : null,
                    del  != null ? del.getName()  : null
            ));
        }


        // Passo 3: Polyline del segmento corrente (opzionale)
        List<VehicleContextDTO.Point> poly = null;

// carica tutti i segmenti della rotta
        List<RealPath> segments = realPathRepository.findByRouteId(routeId);

        if (idx != null && idx >= 0 && steps != null && idx < steps.size() - 1 && segments != null && !segments.isEmpty()) {
            String fromNi = steps.get(idx).getNodeIndex();
            String toNi   = steps.get(idx + 1).getNodeIndex();

            // un solo RealPath per questo arco
            RealPath seg = segments.stream()
                    .filter(s -> fromNi.equals(s.getFromNodeIndex()) && toNi.equals(s.getToNodeIndex()))
                    .findFirst()
                    .orElse(null);

            if (seg != null && seg.getGeometry() != null && !seg.getGeometry().isEmpty()) {
                poly = seg.getGeometry().stream()
                        // geometry è [lon,lat] → converti in (lat,lon)
                        .map(lonlat -> new VehicleContextDTO.Point(lonlat.get(1), lonlat.get(0)))
                        .toList();
            }
        }


        return VehicleContextDTO.builder()
                .vehicleId(vehicleId)
                .routeId(routeId)
                .currentSegmentIndex(idx)
                .nextStop(new VehicleContextDTO.NextStop(nextName, null)) // ETA lo aggiungiamo dopo
                .upcomingStops(upcoming)
                .ordersInProgress(orders)
                .currentSegmentPolyline(poly)
                .build();
    }
}
