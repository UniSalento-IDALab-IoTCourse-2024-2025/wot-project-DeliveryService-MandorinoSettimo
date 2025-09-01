package it.unisalento.pas2425.deliveryserviceproject.service;

import it.unisalento.pas2425.deliveryserviceproject.components.GraphhopperClient;
import it.unisalento.pas2425.deliveryserviceproject.domain.OrderStatus;
import it.unisalento.pas2425.deliveryserviceproject.domain.VehicleStatus;
import it.unisalento.pas2425.deliveryserviceproject.dto.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GreedyRescueService {

    public GreedyRescueResultDTO planGreedy(GreedyInputDTO in) {

        long nowSec = (in.getNow() != null ? in.getNow() : Instant.now()).getEpochSecond();

        Objects.requireNonNull(in, "input");

        if (in.getNodes() == null || in.getVehicles() == null || in.getAllOrders() == null) {
            return new GreedyRescueResultDTO(
                    Collections.emptyList(), "Campi mancanti", GreedyRescueResultDTO.INVALID_REQUEST);
        }

        Map<String, NodeDTO> nodeById = in.getNodes().stream()
                .collect(Collectors.toMap(NodeDTO::getId, n -> n));

        // depot via ID; se non presente, prova a trovarne uno per type=DEPOT
        NodeDTO depot = nodeById.get(in.getDepotNodeId());
        if (depot == null) {
            depot = in.getNodes().stream()
                    .filter(n -> "DEPOT".equalsIgnoreCase(String.valueOf(n.getType())))
                    .findFirst().orElse(null);
        }
        if (depot == null) {
            return new GreedyRescueResultDTO(
                    Collections.emptyList(), "Depot non trovato", GreedyRescueResultDTO.INVALID_REQUEST);
        }

        // ordini del veicolo guasto
        List<OrderDTO> brokenOrders = in.getAllOrders().stream()
                .filter(o -> Objects.equals(o.getAssignedVehicleId(), in.getBrokenVehicleId()))
                .filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS || o.getStatus() == OrderStatus.PICKED_UP)
                .toList();
        if (brokenOrders.isEmpty()) {
            return new GreedyRescueResultDTO(
                    Collections.emptyList(), "Nessun ordine per il veicolo guasto", GreedyRescueResultDTO.NO_ORDERS);
        }

        // candidati = IN_TRANSIT o AVAILABLE
        //todo: se vogliamo scegliere solo quelli AVAILABLE cambiare semplicemente qua
        List<VehicleDTO> candidates = in.getVehicles().stream()
                .filter(v -> v.getStatus() == VehicleStatus.IN_TRANSIT || v.getStatus() == VehicleStatus.AVAILABLE)
                .toList();
        if (candidates.isEmpty()) {
            return new GreedyRescueResultDTO(
                    Collections.emptyList(), "Nessun veicolo candidato", GreedyRescueResultDTO.NO_CANDIDATES);
        }

        // capienza residua (sottrae i PICKED_UP già a bordo)
        //Map<String, Integer> residual = computeResidualCapacity(in.getAllOrders(), in.getVehicles());

        List<RescueAssignmentDTO> assignments = new ArrayList<>();

        for (OrderDTO o : brokenOrders) {
            NodeDTO delivery = nodeById.get(o.getDeliveryNodeId());
            if (delivery == null) {
                return new GreedyRescueResultDTO(
                        Collections.emptyList(), "Delivery node non trovato: " + o.getDeliveryNodeId(),
                        GreedyRescueResultDTO.INVALID_REQUEST);
            }

            if (o.getStatus() == OrderStatus.PICKED_UP) {
                RescueAssignmentDTO a = selectForPickedUp(o, delivery, in.getAnomalyLat(), in.getAnomalyLon(), nowSec, candidates, depot);
                if (a == null) {
                    a = fallbackIgnoringTW(o, null, delivery, in.getAnomalyLat(), in.getAnomalyLon(), candidates, depot);
                }
                if (a != null) assignments.add(a);
            } else {
                NodeDTO pickup = nodeById.get(o.getPickupNodeId());
                if (pickup == null) {
                    return new GreedyRescueResultDTO(Collections.emptyList(), "Pickup node non trovato: " + o.getPickupNodeId(), GreedyRescueResultDTO.INVALID_REQUEST);
                }
                RescueAssignmentDTO a = selectForInProgress(o, pickup, delivery, nowSec, candidates, depot);
                if (a == null) {
                    a = fallbackIgnoringTW(o, pickup, delivery, in.getAnomalyLat(), in.getAnomalyLon(), candidates, depot);
                }
                if (a != null) assignments.add(a);
            }
        }

        if (assignments.isEmpty()) {
            return new GreedyRescueResultDTO(
                    Collections.emptyList(), "Non è stata trovata una soluzione", GreedyRescueResultDTO.NO_SOLUTION);
        }

        return new GreedyRescueResultDTO(assignments, "Soluzione calcolata", GreedyRescueResultDTO.OK);
    }


    /*private static Map<String, Integer> computeResidualCapacity(List<OrderDTO> allOrders, List<VehicleDTO> vehicles) {
        Map<String, Integer> used = new HashMap<>();
        for (VehicleDTO v : vehicles) used.put(v.getId(), 0);
        for (OrderDTO o : allOrders) {
            if (o.getStatus() == OrderStatus.PICKED_UP && o.getAssignedVehicleId() != null) {
                used.compute(o.getAssignedVehicleId(), (k, v) -> (v == null ? 0 : v) + o.getQuantity());
            }
        }
        Map<String, Integer> residual = new HashMap<>();
        for (VehicleDTO v : vehicles) {
            int u = used.getOrDefault(v.getId(), 0);
            residual.put(v.getId(), Math.max(0, v.getCapacity() - u));
        }
        return residual;
    }*/

    private RescueAssignmentDTO selectForPickedUp(
            OrderDTO o, NodeDTO delivery,
            double anomalyLat, double anomalyLon,
            long nowSec,
            List<VehicleDTO> candidates,
            NodeDTO depot
    ) {
        // Candidati con start effettivo
        List<VehicleDTO> feasible = candidates.stream()
                .filter(v -> v.getCapacity() >= o.getQuantity())
                .filter(v ->
                        (v.getStatus() == VehicleStatus.IN_TRANSIT && v.getCurrentLat() != null && v.getCurrentLon() != null)
                                || v.getStatus() == VehicleStatus.AVAILABLE
                )
                .sorted(Comparator.comparingDouble(v -> {
                    double sLat = (v.getStatus() == VehicleStatus.IN_TRANSIT) ? v.getCurrentLat() : depot.getLat();
                    double sLon = (v.getStatus() == VehicleStatus.IN_TRANSIT) ? v.getCurrentLon() : depot.getLon();
                    return haversineKm(sLat, sLon, anomalyLat, anomalyLon); // puoi sostituire con ETA GH
                }))
                .toList();

        for (VehicleDTO v : feasible) {
            double sLat = (v.getStatus() == VehicleStatus.IN_TRANSIT) ? v.getCurrentLat() : depot.getLat();
            double sLon = (v.getStatus() == VehicleStatus.IN_TRANSIT) ? v.getCurrentLon() : depot.getLon();

            double leg1 = haversineKm(sLat, sLon, anomalyLat, anomalyLon);
            double leg2 = haversineKm(anomalyLat, anomalyLon, delivery.getLat(), delivery.getLon());
            double etaMin = leg1 + leg2; // 1 km ≈ 1 min

            if (nowSec + minutesToSeconds(etaMin) <= o.getTwClose()) {
                //residual.compute(v.getId(), (k,r) -> (r==null?0:r) - o.getQuantity());
                return new RescueAssignmentDTO(v.getId(), o.getId(), "PICKED_UP_RESCUE",
                        anomalyLat, anomalyLon, delivery.getLat(), delivery.getLon());
            }
        }
        return null; // nessuno soddisfa TW/capienza
    }

    private RescueAssignmentDTO selectForInProgress(
            OrderDTO o, NodeDTO pickup, NodeDTO delivery,
            long nowSec,
            List<VehicleDTO> candidates,
            NodeDTO depot
    ) {
        List<VehicleDTO> feasible = candidates.stream()
                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)   // ← SOLO AVAILABLE per IN_PROGRESS
                .filter(v -> v.getCapacity() >= o.getQuantity())
                .sorted(Comparator.comparingDouble(v -> {
                    double sLat = depot.getLat(), sLon = depot.getLon();
                    return haversineKm(sLat, sLon, pickup.getLat(), pickup.getLon());
                }))
                .toList();

        for (VehicleDTO v : feasible) {
            double sLat = (v.getStatus() == VehicleStatus.IN_TRANSIT) ? v.getCurrentLat() : depot.getLat();
            double sLon = (v.getStatus() == VehicleStatus.IN_TRANSIT) ? v.getCurrentLon() : depot.getLon();

            double leg1 = haversineKm(sLat, sLon, pickup.getLat(), pickup.getLon());
            double leg2 = haversineKm(pickup.getLat(), pickup.getLon(), delivery.getLat(), delivery.getLon());
            double etaMin = leg1 + leg2;

            if (nowSec + minutesToSeconds(etaMin) <= o.getTwClose()) {
                //residual.compute(v.getId(), (k,r) -> (r==null?0:r) - o.getQuantity());
                return new RescueAssignmentDTO(v.getId(), o.getId(), "IN_PROGRESS_REASSIGN",
                        pickup.getLat(), pickup.getLon(), delivery.getLat(), delivery.getLon());
            }
        }
        return null;
    }

    private RescueAssignmentDTO fallbackIgnoringTW(
            OrderDTO o,
            NodeDTO pickup, NodeDTO delivery,
            double anomalyLat, double anomalyLon,
            List<VehicleDTO> candidates,
            NodeDTO depot) {

        record Start(VehicleDTO v, double lat, double lon, String mode) {}
        List<Start> starts = new ArrayList<>();

        for (VehicleDTO v : candidates) {
            if (v.getCapacity() < o.getQuantity()) continue;

            if (v.getStatus() == VehicleStatus.IN_TRANSIT &&
                    v.getCurrentLat() != null && v.getCurrentLon() != null) {
                starts.add(new Start(v, v.getCurrentLat(), v.getCurrentLon(), "IN_TRANSIT"));
            } else if (v.getStatus() == VehicleStatus.AVAILABLE) {
                starts.add(new Start(v, depot.getLat(), depot.getLon(), "AVAILABLE"));
            }
        }

        if (starts.isEmpty()) return null;

        // distanza da dove parte allo “start” corretto
        Comparator<Start> byDist = (s1, s2) -> {
            double d1 = (o.getStatus() == OrderStatus.PICKED_UP)
                    ? haversineKm(s1.lat, s1.lon, anomalyLat, anomalyLon)
                    : haversineKm(s1.lat, s1.lon, pickup.getLat(), pickup.getLon());
            double d2 = (o.getStatus() == OrderStatus.PICKED_UP)
                    ? haversineKm(s2.lat, s2.lon, anomalyLat, anomalyLon)
                    : haversineKm(s2.lat, s2.lon, pickup.getLat(), pickup.getLon());
            return Double.compare(d1, d2);
        };

        Start best = starts.stream().min(byDist).orElse(null);
        if (best == null) return null;

        //residual.put(best.v.getId(), residual.get(best.v.getId()) - o.getQuantity());

        if (o.getStatus() == OrderStatus.PICKED_UP) {
            return new RescueAssignmentDTO(
                    best.v.getId(), o.getId(), "PICKED_UP_RESCUE",
                    anomalyLat, anomalyLon,
                    delivery.getLat(), delivery.getLon());
        } else {
            return new RescueAssignmentDTO(
                    best.v.getId(), o.getId(), "IN_PROGRESS_REASSIGN",
                    pickup.getLat(), pickup.getLon(),
                    delivery.getLat(), delivery.getLon());
        }
    }



    // ---------- helpers ----------
    private static double distanceKm(VehicleDTO v, double lat, double lon) {
        double vlat = (v.getStatus() == VehicleStatus.IN_TRANSIT && v.getCurrentLat() != null) ? v.getCurrentLat() : 0.0;
        double vlon = (v.getStatus() == VehicleStatus.IN_TRANSIT && v.getCurrentLon() != null) ? v.getCurrentLon() : 0.0;
        return haversineKm(vlat, vlon, lat, lon);
    }

    private static long minutesToSeconds(double m) { return (long) Math.round(m * 60.0); }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }


}
