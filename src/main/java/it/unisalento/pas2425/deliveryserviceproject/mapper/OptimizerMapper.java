package it.unisalento.pas2425.deliveryserviceproject.mapper;

import it.unisalento.pas2425.deliveryserviceproject.domain.Node;
import it.unisalento.pas2425.deliveryserviceproject.domain.Vehicle;
import it.unisalento.pas2425.deliveryserviceproject.domain.VehicleStatus;
import it.unisalento.pas2425.deliveryserviceproject.dto.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OptimizerMapper {

    public static Map<String, Integer> generateIndexMap(List<String> ids) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            map.put(ids.get(i), i);
        }
        return map;
    }

    public static List<NodeDTO> mapNodes(List<NodeDTO> nodes, Map<String, Integer> idMap) {
        return nodes.stream().map(n -> {
            NodeDTO dto = new NodeDTO();
            dto.setId(String.valueOf(idMap.get(n.getId())));
            dto.setName(n.getName());
            dto.setLat(n.getLat());
            dto.setLon(n.getLon());
            dto.setType(n.getType());
            dto.setAddress(n.getAddress());
            return dto;
        }).toList();
    }

    public static List<OrderDTO> mapOrders(List<OrderDTO> orders, Map<String, Integer> nodeMap,
                                     Map<String, Integer> orderMap, Map<String, Integer> vehicleMap) {
        return orders.stream().map(o -> {
            OrderDTO dto = new OrderDTO();
            dto.setId(String.valueOf(orderMap.get(o.getId())));
            dto.setPickupNodeId(String.valueOf(nodeMap.get(o.getPickupNodeId())));
            dto.setDeliveryNodeId(String.valueOf(nodeMap.get(o.getDeliveryNodeId())));
            dto.setQuantity(o.getQuantity());
            dto.setTwOpen(o.getTwOpen());
            dto.setTwClose(o.getTwClose());
            if (o.getAssignedVehicleId() != null)
                dto.setAssignedVehicleId(String.valueOf(vehicleMap.get(o.getAssignedVehicleId())));
            dto.setVehicleRouteId(o.getVehicleRouteId());
            return dto;
        }).toList();
    }

    public static List<VehicleDTO> mapVehicles(List<Vehicle> vehicles, Map<String, Integer> idMap) {
        return vehicles.stream().map(v -> {
            VehicleDTO dto = new VehicleDTO();
            dto.setId(String.valueOf(idMap.get(v.getId())));
            dto.setPlate(v.getPlate());
            dto.setCapacity(v.getCapacity());
            dto.setCost(v.getCost());
            dto.setCurrentLat(v.getCurrentLat());
            dto.setCurrentLon(v.getCurrentLon());
            dto.setStatus(VehicleStatus.valueOf(v.getStatus().name()));
            return dto;
        }).toList();
    }

    public static void remapAssignedOrders(OptimizerResultDTO result,
                                           Map<String, Integer> orderMap,
                                           Map<String, Integer> nodeMap,
                                           Map<String, Integer> vehicleMap) {

        Map<Integer, String> orderIdReverse = reverseMap(orderMap);
        Map<Integer, String> nodeIdReverse = reverseMap(nodeMap);
        Map<Integer, String> vehicleIdReverse = reverseMap(vehicleMap);

        if (result.getSolution() != null && result.getSolution().getAssignedOrders() != null) {
            for (AssignedOrderDTO a : result.getSolution().getAssignedOrders()) {
                a.setOrderId(resolveReverse(a.getOrderId(), orderIdReverse));
                a.setPickupNodeId(resolveReverse(a.getPickupNodeId(), nodeIdReverse));
                a.setDeliveryNodeId(resolveReverse(a.getDeliveryNodeId(), nodeIdReverse));
                a.setAssignedVehicleId(resolveReverse(a.getAssignedVehicleId(), vehicleIdReverse));
            }
        }
    }

    private static String resolveReverse(String key, Map<Integer, String> reverseMap) {
        try {
            return reverseMap.getOrDefault(Integer.parseInt(key), key);
        } catch (NumberFormatException e) {
            return key;
        }
    }


    public static Map<Integer, String> reverseMap(Map<String, Integer> original) {
        return original.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    public static void remapGeoRoutes(List<RealPathDTO> geoRoutes, Map<Integer, String> nodeIdReverse, Map<Integer, String> vehicleIdReverse) {
        for (RealPathDTO geo : geoRoutes) {
            if (geo.getFromNodeIndex() != null) {
                Integer fromIdx = Integer.valueOf(geo.getFromNodeIndex().toString());
                geo.setFromNodeIndex(nodeIdReverse.get(fromIdx));
            }
            if (geo.getToNodeIndex() != null) {
                Integer toIdx = Integer.valueOf(geo.getToNodeIndex().toString());
                geo.setToNodeIndex(nodeIdReverse.get(toIdx));
            }
            if (geo.getVehicleId() != null) {
                try {
                    int vehicleIdx = Integer.parseInt(geo.getVehicleId());
                    geo.setVehicleId(vehicleIdReverse.getOrDefault(vehicleIdx, geo.getVehicleId()));
                } catch (NumberFormatException e) {
                    // vehicleId è già una stringa valida, lascialo com'è
                }
            }
        }
    }

    public static void remapSolutionPath(OptimizerResultDTO result,
                                         Map<Integer, String> nodeIdReverse,
                                         Map<Integer, String> vehicleIdReverse) {
        if (result.getSolution() != null && result.getSolution().getPath() != null) {
            for (VehicleRouteDTO path : result.getSolution().getPath()) {
                try {
                    int vehId = Integer.parseInt(path.getVehicleId());
                    path.setVehicleId(vehicleIdReverse.getOrDefault(vehId, path.getVehicleId()));
                } catch (NumberFormatException e) {
                    // Già stringa
                }

                if (path.getRoute() != null) {
                    for (RouteNodeDTO node : path.getRoute()) {
                        if (node.getNodeIndex() != null) {
                            try {
                                int nodeIdx = Integer.parseInt(node.getNodeIndex().toString());
                                node.setNodeIndex(nodeIdReverse.getOrDefault(nodeIdx, node.getNodeIndex()));
                            } catch (NumberFormatException e) {
                                // Già mappato
                            }
                        }
                    }
                }
            }
        }
    }


}
