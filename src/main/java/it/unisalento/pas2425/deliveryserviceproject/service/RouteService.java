package it.unisalento.pas2425.deliveryserviceproject.service;

import it.unisalento.pas2425.deliveryserviceproject.components.GraphhopperClient;
import it.unisalento.pas2425.deliveryserviceproject.components.MqttPublisher;
import it.unisalento.pas2425.deliveryserviceproject.components.OptimizerClient;
import it.unisalento.pas2425.deliveryserviceproject.components.PositionClient;
import it.unisalento.pas2425.deliveryserviceproject.domain.*;
import it.unisalento.pas2425.deliveryserviceproject.dto.*;
import it.unisalento.pas2425.deliveryserviceproject.mapper.RealPathMapper;
import it.unisalento.pas2425.deliveryserviceproject.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unisalento.pas2425.deliveryserviceproject.mapper.OptimizerMapper.*;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    @Autowired
    private OptimizerClient optimizerClient;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private RealPathRepository realPathRepository;

    @Autowired
    private VehicleRouteRepository vehicleRouteRepository;

    @Autowired
    private PositionClient positionClient;

    @Autowired
    private GraphhopperClient graphhopperClient;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private GreedyRescueService greedyRescueService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private MqttPublisher mqttPublisher;

    public OptimizerResultDTO optimize(OrderListDTO dto, String jwtToken) {
        OptimizerResultDTO result = new OptimizerResultDTO();
        List<OrderDTO> orders = dto.getOrders();

        if (orders == null || orders.isEmpty()) {
            result.setCode(OptimizerResultDTO.MISSING_DATA);
            result.setMessage("Lista ordini vuota");
            return result;
        }

        // 1. Estrai tutti i nodeId coinvolti (pickup e delivery)
        Set<String> nodeIds = orders.stream()
                .flatMap(o -> Stream.of(o.getPickupNodeId(), o.getDeliveryNodeId()))
                .collect(Collectors.toSet());

        // 2. Estrai gli id veicoli usati negli ordini (se presenti)
        Set<String> vehicleIds = orders.stream()
                .map(OrderDTO::getAssignedVehicleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. Fetch nodi dal DB
        List<Node> nodes = nodeRepository.findAllById(nodeIds);
        Optional<Node> depotOpt = nodeRepository.findFirstByType(NodeType.DEPOT);
        depotOpt.ifPresent(d -> {
            if (nodes.stream().noneMatch(n -> n.getId().equals(d.getId()))) {
                nodes.add(d);
            }
        });

        // Se non ci sono nodi, ritorna errore
        if (nodes.isEmpty()) {
            result.setCode(OptimizerResultDTO.MISSING_DATA);
            result.setMessage("Nessun nodo trovato per gli ordini");
            return result;
        }

        // 4. Fetch veicoli disponibili + quelli assegnati esplicitamente
        List<Vehicle> vehicles = vehicleRepository.findAll().stream()
                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE || vehicleIds.contains(v.getId()))
                .toList();

        if (vehicles.isEmpty()) {
            result.setCode(OptimizerResultDTO.NO_AVAILABLE_VEHICLES);
            result.setMessage("Nessun veicolo disponibile");
            return result;
        }

        if (nodes.isEmpty()) {
            result.setCode(OptimizerResultDTO.MISSING_DATA);
            result.setMessage("Nodi non trovati");
            return result;
        }

        // 5. Deduplica nodi se compaiono più volte come pickup/delivery
        DeduplicationResult dedup = deduplicateNodesAndAdjustOrders(orders, nodes);
        List<NodeDTO> deduplicatedNodes = dedup.nodes;
        Map<String, String> duplicatedToOriginalNodeId = dedup.duplicatedToOriginal;

        // Assicurati che il DEPOT sia tra i NodeDTO deduplicati
        depotOpt.ifPresent(depot -> {
            deduplicatedNodes.add(new NodeDTO(
                    depot.getId(),
                    depot.getName(),
                    depot.getAddress(),
                    depot.getLat(),
                    depot.getLon(),
                    depot.getType()
            ));
        });

        // 6. Nuovo mapping ID basato su nodi deduplicati
        Map<String, Integer> nodeIdMap = generateIndexMap(deduplicatedNodes.stream().map(NodeDTO::getId).toList());
        Map<String, Integer> vehicleIdMap = generateIndexMap(vehicles.stream().map(Vehicle::getId).toList());
        Map<String, Integer> orderIdMap = generateIndexMap(orders.stream().map(OrderDTO::getId).toList());

        Map<Integer, String> nodeIdReverse = new HashMap<>();
        for (Map.Entry<String, Integer> entry : nodeIdMap.entrySet()) {
            String currentId = entry.getKey();
            int index = entry.getValue();

            if (duplicatedToOriginalNodeId.containsKey(currentId)) {
                nodeIdReverse.put(index, duplicatedToOriginalNodeId.get(currentId));
            } else {
                nodeIdReverse.put(index, currentId);
            }
        }

        // 7. Costruzione DTO finale
        OptimizeRequestDTO requestDTO = new OptimizeRequestDTO();
        requestDTO.setNodes(mapNodes(deduplicatedNodes, nodeIdMap));
        requestDTO.setOrders(mapOrders(orders, nodeIdMap, orderIdMap, vehicleIdMap));
        requestDTO.setVehicles(mapVehicles(vehicles, vehicleIdMap));

        // 8. Chiamata a Python
        result = optimizerClient.callPythonOptimizer(requestDTO);

        if (result == null || result.getSolution() == null) {
            result.setCode(OptimizerResultDTO.NO_SOLUTION);
            result.setMessage("Nessuna soluzione trovata");
            return result;
        }

        // 🔍 Conta i veicoli unici usati nella soluzione
        Set<String> usedVehicleIds = result.getSolution().getAssignedOrders().stream()
                .map(AssignedOrderDTO::getAssignedVehicleId)
                .collect(Collectors.toSet());

        // 🔁 Chiamata al microservizio PositionService per contare gli utenti disponibili
        int availableUsers = positionClient.countAvailableUsers(jwtToken);
        System.out.println("Utenti disponibili: " + availableUsers);
        // Se non ci sono abbastanza utenti disponibili → errore
        if (availableUsers < usedVehicleIds.size()) {
            result.setCode(OptimizerResultDTO.NOT_ENOUGH_USERS);
            result.setMessage("Non ci sono abbastanza utenti disponibili per coprire tutte le tratte");
            return result;
        }

        // 9. Riconversione ID int → string
        remapAssignedOrders(result, orderIdMap, nodeIdMap, vehicleIdMap);
        remapSolutionPath(result, nodeIdReverse, reverseMap(vehicleIdMap));
        List<VehicleRoute> routeEntities = result.getSolution().getPath().stream()
                .map(route -> {
                    VehicleRoute entity = new VehicleRoute();
                    entity.setVehicleId(route.getVehicleId());
                    entity.setRoute(route.getRoute());
                    entity.setCompleted(false); // Imposta completed a false
                    return entity;
                }).toList();

        //salvataggio delle rotte utili: solo quelle con più di 2 nodi
        List<VehicleRoute> savedRoutes = vehicleRouteRepository.saveAll(
                routeEntities.stream()
                        .filter(r -> r.getRoute().size() > 2)
                        .toList()
        );

        Map<String, String> routeIdMap = savedRoutes.stream()
                .collect(Collectors.toMap(VehicleRoute::getVehicleId, VehicleRoute::getId));

        for (VehicleRouteDTO vdto : result.getSolution().getPath()) {
            vdto.setId(routeIdMap.get(vdto.getVehicleId()));
        }

        // 10. Aggiorna gli ordini nel database
        for (AssignedOrderDTO assigned : result.getSolution().getAssignedOrders()) {
            Optional<Order> optionalOrder = orderRepository.findById(assigned.getOrderId());
            if (optionalOrder.isPresent()) {
                Order order = optionalOrder.get();
                order.setAssignedVehicleId(assigned.getAssignedVehicleId());
                order.setStatus(OrderStatus.ASSIGNED);

                // ✅ Aggiungi qui il vehicleRouteId
                String vehicleRouteId = routeIdMap.get(assigned.getAssignedVehicleId());
                if (vehicleRouteId != null) {
                    order.setVehicleRouteId(vehicleRouteId);
                }

                orderRepository.save(order);
            }
        }

        // 11. Aggiorna i veicoli nel database
        if (depotOpt.isPresent()) {
            Node d = depotOpt.get();
            for (String vehicleId : result.getSolution().getAssignedOrders().stream()
                    .map(AssignedOrderDTO::getAssignedVehicleId)
                    .collect(Collectors.toSet())) {

                vehicleRepository.findById(vehicleId).ifPresent(v -> {
                    v.setCurrentLat(d.getLat());
                    v.setCurrentLon(d.getLon());
                    vehicleRepository.save(v);
                });
            }
        } else {
            result.setCode(OptimizerResultDTO.MISSING_DEPOT);
            result.setMessage("Depot non trovato nel database");
            return result;
        }

        // 12. Salva i nodi deduplicati nel database
        remapGeoRoutes(result.getGeoRoutes(), nodeIdReverse, reverseMap(vehicleIdMap));

        List<Order> updatedOrders = orderRepository.findAllById(
                result.getSolution().getAssignedOrders().stream()
                        .map(AssignedOrderDTO::getOrderId)
                        .toList()
        );
        // Crea una mappa per accedere rapidamente agli ordini aggiornati
        Map<String, List<Order>> ordersByPickupKey = updatedOrders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getVehicleRouteId() + "_" + o.getAssignedVehicleId() + "_" + o.getPickupNodeId()
                ));

        Map<String, List<Order>> ordersByDeliveryKey = updatedOrders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getVehicleRouteId() + "_" + o.getAssignedVehicleId() + "_" + o.getDeliveryNodeId()
                ));



        for (RealPathDTO geo : result.getGeoRoutes()) {
            String routeId = routeIdMap.get(geo.getVehicleId());
            if (routeId != null) geo.setRouteId(routeId);

            String key = geo.getRouteId() + "_" + geo.getVehicleId() + "_" + geo.getToNodeIndex();

            // raccogliamo in un set per evitare duplicati
            Set<String> ids = new LinkedHashSet<>();
            if (geo.getOrderIds() != null) ids.addAll(geo.getOrderIds());

            if ("Pickup".equalsIgnoreCase(geo.getToLabel())) {
                List<Order> matching = ordersByPickupKey.getOrDefault(key, Collections.emptyList());
                ids.addAll(
                        matching.stream()
                                .filter(o -> Set.of(OrderStatus.PENDING, OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS)
                                        .contains(o.getStatus()))
                                .map(Order::getId)
                                .toList()
                );
            }

            if ("Delivery".equalsIgnoreCase(geo.getToLabel())) {
                List<Order> matching = ordersByDeliveryKey.getOrDefault(key, Collections.emptyList());
                ids.addAll(
                        matching.stream()
                                .filter(o -> Set.of(OrderStatus.PENDING, OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS, OrderStatus.PICKED_UP)
                                        .contains(o.getStatus()))
                                .map(Order::getId)
                                .toList()
                );
            }

            geo.setOrderIds(new ArrayList<>(ids));
        }

        List<RealPath> paths = RealPathMapper.fromDTOList(result.getGeoRoutes())
                .stream()
                .filter(p -> p.getGeometry() != null && p.getGeometry().size() > 2)
                .toList();
        realPathRepository.saveAll(paths);

        result.setGeoRoutes(RealPathMapper.toRealPathDTO(paths));
        result.setCode(OptimizerResultDTO.OK);
        result.setMessage("Ottimizzazione completata con successo");
        return result;
    }

    private static class DeduplicationResult {
        List<NodeDTO> nodes;
        Map<String, String> duplicatedToOriginal;
    }


    private DeduplicationResult deduplicateNodesAndAdjustOrders(List<OrderDTO> orders, List<Node> nodes) {
        Map<String, Node> nodeMap = nodes.stream()
                .collect(Collectors.toMap(Node::getId, Function.identity()));

        int fakePickupIdCounter = 10000;
        int fakeDeliveryIdCounter = 20000;

        List<NodeDTO> finalNodeDTOs = new ArrayList<>();
        Map<String, String> duplicatedToOriginalNodeId = new HashMap<>();

        Set<String> usedAsPickup = new HashSet<>();
        Set<String> usedAsDelivery = new HashSet<>();

        for (OrderDTO order : orders) {
            // === PICKUP ===
            String pickupId = order.getPickupNodeId();
            if (usedAsPickup.contains(pickupId) || usedAsDelivery.contains(pickupId)) {
                String newPickupId = String.valueOf(fakePickupIdCounter++);
                Node original = nodeMap.get(pickupId);
                NodeDTO duplicate = new NodeDTO(newPickupId, original.getName(), original.getAddress(),
                        original.getLat(), original.getLon(), original.getType());
                finalNodeDTOs.add(duplicate);
                duplicatedToOriginalNodeId.put(newPickupId, pickupId); // 👈 salvataggio relazione
                order.setPickupNodeId(newPickupId);
            } else {
                usedAsPickup.add(pickupId);
            }

            // === DELIVERY ===
            String deliveryId = order.getDeliveryNodeId();
            if (usedAsPickup.contains(deliveryId) || usedAsDelivery.contains(deliveryId)) {
                String newDeliveryId = String.valueOf(fakeDeliveryIdCounter++);
                Node original = nodeMap.get(deliveryId);
                NodeDTO duplicate = new NodeDTO(newDeliveryId, original.getName(), original.getAddress(),
                        original.getLat(), original.getLon(), original.getType());
                finalNodeDTOs.add(duplicate);
                duplicatedToOriginalNodeId.put(newDeliveryId, deliveryId); // 👈 salvataggio relazione
                order.setDeliveryNodeId(newDeliveryId);
            } else {
                usedAsDelivery.add(deliveryId);
            }
        }

        // Aggiungi tutti i nodi usati (originali) che non sono stati duplicati
        Set<String> allUsed = Stream.concat(usedAsPickup.stream(), usedAsDelivery.stream()).collect(Collectors.toSet());
        for (String id : allUsed) {
            if (finalNodeDTOs.stream().noneMatch(n -> n.getId().equals(id))) {
                Node n = nodeMap.get(id);
                finalNodeDTOs.add(new NodeDTO(n.getId(), n.getName(), n.getAddress(), n.getLat(), n.getLon(), n.getType()));
            }
        }

        DeduplicationResult result = new DeduplicationResult();
        result.nodes = finalNodeDTOs;
        result.duplicatedToOriginal = duplicatedToOriginalNodeId;
        return result;
    }

    public RouteResultDTO completeRoute(String routeId, String jwtToken) {
        RouteResultDTO result = new RouteResultDTO();
        Optional<VehicleRoute> routeOpt = vehicleRouteRepository.findById(routeId);
        if (routeOpt.isEmpty()) {
            result.setCode(RouteResultDTO.ROUTE_NOT_FOUND);
            result.setMessage("Tratta non trovata");
            return result;
        }
        VehicleRoute route = routeOpt.get();

        // 🔁 Imposta route come completata
        route.setCompleted(true);
        route.setVehicleStatus(VehicleStatus.OFFLINE);
        vehicleRouteRepository.save(route);
        mqttPublisher.clearRouteStarted(route.getVehicleId());

        // 🛻 Resetta veicolo
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(route.getVehicleId());
        if (vehicleOpt.isEmpty()) {
            result.setCode(RouteResultDTO.VEHICLE_NOT_FOUND);
            result.setMessage("Veicolo associato alla tratta non trovato");
            return result;
        }

        Vehicle vehicle = vehicleOpt.get();
        if (vehicle.getAssignedUserId() != null) {

            // Rendi disponibile l'utente (camionista)
            UserResultDTO positionResult = positionClient.updateUserStatus(vehicle.getAssignedUserId(), UserStatus.AVAILABLE, jwtToken);
            if (positionResult.getCode() == UserResultDTO.OK) {
                vehicle.setAssignedUserId("");
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(vehicle);
                result.setCode(RouteResultDTO.OK);
                result.setMessage("Utente assegnato e veicolo aggiornato con successo");
                return result;

            } else {
                result.setCode(RouteResultDTO.USER_UPDATE_ERROR);
                result.setMessage("Veicolo aggiornato, ma errore nel cambio stato utente: " + positionResult.getMessage());
                return result;
            }

        } else {
            result.setCode(RouteResultDTO.VEHICLE_NOT_MODIFIED);
            result.setMessage("Veicolo e utente non modificati");
            return result;
        }

    }

    public RecalculateRouteResultDTO recalculateRoute(RecalculateRouteRequestDTO request, String token) {
        RecalculateRouteResultDTO result = new RecalculateRouteResultDTO();

        RealPath currentSegment = realPathRepository.findById(request.getSegmentId()).orElse(null);
        if (currentSegment == null) {
            result.setCode(RecalculateRouteResultDTO.SEGMENT_NOT_FOUND);
            result.setMessage("Segmento non trovato");
            return result;
        }

        String toIdx = currentSegment.getToNodeIndex();

// 1) prova nodo “reale” in tabella Node
        Double destLat = null, destLon = null;
        Node destination = nodeRepository.findById(toIdx).orElse(null);
        if (destination != null) {
            destLat = destination.getLat();
            destLon = destination.getLon();
        }

// 2) fallback: nodo “sintetico” dentro la VehicleRoute
        if (destLat == null || destLon == null) {
            VehicleRoute vr = vehicleRouteRepository.findById(currentSegment.getRouteId()).orElse(null);
            if (vr != null && vr.getRoute() != null) {
                RouteNodeDTO target = vr.getRoute().stream()
                        .filter(n -> Objects.equals(toIdx, n.getNodeIndex()))
                        .findFirst().orElse(null);
                if (target != null) {
                    destLat = target.getLat();
                    destLon = target.getLon();
                }
            }
        }

// 3) (opzionale) ulteriore fallback per RESCUE_<orderId>
        if ((destLat == null || destLon == null) && toIdx != null && toIdx.startsWith("RESCUE_")) {
            String orderId = toIdx.substring("RESCUE_".length());
            Order ord = orderRepository.findById(orderId).orElse(null);
            if (ord != null) {
                Node del = nodeRepository.findById(ord.getDeliveryNodeId()).orElse(null);
                if (del != null) {
                    destLat = del.getLat();
                    destLon = del.getLon();
                }
            }
        }

        if (destLat == null || destLon == null) {
            result.setCode(RecalculateRouteResultDTO.DESTINATION_NODE_NOT_FOUND);
            result.setMessage("Nodo di destinazione non trovato (né tra Node né tra i RouteNode della route).");
            return result;
        }

// GH route dal punto corrente al to-node della tratta attiva
        List<List<Double>> newGeometry;
        try {
            newGeometry = graphhopperClient.getRoute(
                    request.getCurrentLat(), request.getCurrentLon(), destLat, destLon
            );
        } catch (Exception e) {
            result.setCode(RecalculateRouteResultDTO.GRAPHOPPER_ERROR);
            result.setMessage("Errore nella chiamata a Graphhopper: " + e.getMessage());
            return result;
        }

        double distance = graphhopperClient.getDistance();
        double time = graphhopperClient.getTime();

        RealPathDTO updated = new RealPathDTO();
        updated.setId(currentSegment.getId());
        updated.setGeometry(newGeometry);
        updated.setDistanceM(distance);
        updated.setTimeS(time);

        result.setCode(RecalculateRouteResultDTO.OK);
        result.setMessage("Ricalcolo completato");
        result.setUpdatedSegment(updated);
        return result;
    }

    public RouteResultDTO handleAnomalyGreedy(ReportAnomalyRequestDTO request, String jwtToken) {
        RouteResultDTO resp = new RouteResultDTO();
        try {
            if (request == null || request.getVehicleId() == null) {
                resp.setCode(RouteResultDTO.INVALID_REQUEST);
                resp.setMessage("vehicleId mancante");
                return resp;
            }

            // 1) Lookup broken vehicle & mark as BROKEN
            Vehicle broken = vehicleRepository.findById(request.getVehicleId()).orElse(null);
            if (broken == null) {
                resp.setCode(RouteResultDTO.VEHICLE_NOT_FOUND);
                resp.setMessage("Veicolo non trovato");
                return resp;
            }
            broken.setStatus(VehicleStatus.BROKEN);
            vehicleRepository.save(broken);

            if (request.getActiveRouteId() != null) {
                vehicleRouteRepository.findById(request.getActiveRouteId()).ifPresent(r -> {
                    if (!r.isCompleted()) {
                        r.setCompleted(true);
                        r.setVehicleStatus(VehicleStatus.BROKEN);
                        r.setAssociatedUserId(null);
                        vehicleRouteRepository.save(r);
                        mqttPublisher.clearRouteStarted(r.getVehicleId());
                    }
                });
            }

            if(request.getUserId() != null) {
                UserResultDTO positionResult = positionClient.updateUserStatus(request.getUserId(), UserStatus.UNAVAILABLE, jwtToken);
                if (positionResult.getCode() != UserResultDTO.OK) {
                    resp.setCode(RouteResultDTO.USER_UPDATE_ERROR);
                    resp.setMessage("Errore nel cambio stato utente: " + positionResult.getMessage());
                    return resp;
                }
            }

            // 2) Posizione anomalia (request > vehicle.current)
            Double anomalyLat = (request.getAnomalyLat() != null) ? request.getAnomalyLat() : broken.getCurrentLat();
            Double anomalyLon = (request.getAnomalyLon() != null) ? request.getAnomalyLon() : broken.getCurrentLon();
            if (anomalyLat == null || anomalyLon == null) {
                resp.setCode(RouteResultDTO.INVALID_REQUEST);
                resp.setMessage("Posizione anomalia mancante");
                return resp;
            }

            // 3) ENTITÀ → DTO (usa mapper dei tuoi Service)
            // a) tutti i PICKED_UP della flotta (per capacità residua realistica)
            List<OrderDTO> pickedUpAcrossFleet = orderRepository.findByStatus(OrderStatus.PICKED_UP).stream()
                    .map(orderService::toDTO)
                    .toList();

            // b) ordini attivi del veicolo guasto (quelli da riassegnare)
            List<OrderDTO> brokenActive = orderRepository.findByAssignedVehicleIdAndStatusIn(request.getVehicleId(),
                            List.of(OrderStatus.IN_PROGRESS, OrderStatus.PICKED_UP))
                    .stream()
                    .map(orderService::toDTO)
                    .toList();

            if (brokenActive.isEmpty()) {
                resp.setCode(RouteResultDTO.ROUTE_NOT_FOUND);
                resp.setMessage("Nessun ordine attivo da riassegnare");
                return resp;
            }

            // c) unisci evitando duplicati (caso in cui un ordine del rotto sia anche PICKED_UP)
            Map<String, OrderDTO> merged = new LinkedHashMap<>();
            pickedUpAcrossFleet.forEach(o -> merged.put(o.getId(), o));
            brokenActive.forEach(o -> merged.put(o.getId(), o));
            List<OrderDTO> allOrdersForGreedy = new ArrayList<>(merged.values());

            // 4) VEICOLI → DTO (tutti, filtrerà il service)
            List<VehicleDTO> vehiclesDTO = vehicleRepository.findAll().stream()
                    .map(vehicleService::toDTO)
                    .toList();

            // 5) DEPOT unico (ricavalo dai nodi/DB)
            String depotNodeId = nodeRepository.findFirstByType(NodeType.DEPOT).map(Node::getId).orElse(null);
            if (depotNodeId == null) {
                depotNodeId = nodeRepository.findAll().stream()
                        .map(nodeService::toDTO)
                        .filter(n -> n.getType() == NodeType.DEPOT)
                        .map(NodeDTO::getId)
                        .findFirst()
                        .orElse(null);
            }
            if (depotNodeId == null) {
                resp.setCode(RouteResultDTO.INVALID_REQUEST);
                resp.setMessage("Depot non trovato");
                return resp;
            }

            // 6) NODI MINIMI → DTO (solo quelli necessari + DEPOT)
            Set<String> neededNodeIds = new LinkedHashSet<>();
            for (OrderDTO o : brokenActive) {
                neededNodeIds.add(o.getPickupNodeId());
                neededNodeIds.add(o.getDeliveryNodeId());
            }
            neededNodeIds.add(depotNodeId);

            List<NodeDTO> nodesDTO = nodeRepository.findAllById(neededNodeIds).stream()
                    .map(nodeService::toDTO)
                    .toList();

            // 7) now come Instant
            Instant now = (request.getTimestamp() != null) ? request.getTimestamp() : Instant.now();

            // 8) Costruisci l’input per il GREEDY
            GreedyInputDTO input = new GreedyInputDTO(allOrdersForGreedy, vehiclesDTO, nodesDTO, request.getVehicleId(), anomalyLat, anomalyLon, depotNodeId, now);

            // 9) Chiama il GREEDY
            GreedyRescueResultDTO plan = greedyRescueService.planGreedy(input);

            if (plan.getCode() != GreedyRescueResultDTO.OK) {
                resp.setCode(RouteResultDTO.NO_SOLUTION);
                resp.setMessage("Nessuna riassegnazione fattibile (greedy)");
                System.out.println("Soluzione non trovata");
                return resp;
            }
            System.out.println("Soluzione riassegnazione");

            //  PERSISTENZA ORDINI + COSTRUZIONE PAYLOAD

// map veloci
            Map<String, VehicleDTO> vehicleById = vehiclesDTO.stream()
                    .collect(Collectors.toMap(VehicleDTO::getId, v -> v));
            Map<String, OrderDTO> orderById = brokenActive.stream()
                    .collect(Collectors.toMap(OrderDTO::getId, o -> o));
            Map<String, NodeDTO> nodeById = nodesDTO.stream()
                    .collect(Collectors.toMap(NodeDTO::getId, n -> n));
// 0) depot NodeDTO (evita lambda non-final)
            NodeDTO depotNode = null;
            for (NodeDTO n : nodesDTO) {
                if (Objects.equals(n.getId(), depotNodeId)) { depotNode = n; break; }
            }
            if (depotNode == null) {
                resp.setCode(RouteResultDTO.DEPOT_NOT_FOUND);
                resp.setMessage("Depot non trovato in nodesDTO");
                return resp;
            }

// 1) aggiorna ORDINI (DB)
            for (RescueAssignmentDTO a : plan.getAssignments()) {
                Order ord = orderRepository.findById(a.getOrderId()).orElse(null);
                if (ord == null) {
                    resp.setCode(RouteResultDTO.PERSISTENCE_ERROR);
                    resp.setMessage("Ordine non trovato: " + a.getOrderId());
                    return resp;
                }
                ord.setAssignedVehicleId(a.getVehicleId());
                ord.setStatus("PICKED_UP_RESCUE".equals(a.getMode()) ? OrderStatus.PICKED_UP : OrderStatus.IN_PROGRESS);
                try {
                    orderRepository.save(ord);
                } catch (Exception ex) {
                    resp.setCode(RouteResultDTO.PERSISTENCE_ERROR);
                    resp.setMessage("Errore salvataggio ordine " + a.getOrderId() + ": " + ex.getMessage());
                    return resp;
                }
            }

            Map<String, RescueAssignmentDTO> byOrder = new LinkedHashMap<>();
            for (RescueAssignmentDTO a : plan.getAssignments()) byOrder.putIfAbsent(a.getOrderId(), a);
            List<RescueAssignmentDTO> unique = new ArrayList<>(byOrder.values());
// 2) raggruppa assignment per veicolo
            Map<String, List<RescueAssignmentDTO>> perVehicle =
                    unique.stream().collect(Collectors.groupingBy(RescueAssignmentDTO::getVehicleId, LinkedHashMap::new, Collectors.toList()));
            // per edge→order mapping (solo per brokenActive, bastano questi)
// output
            List<VehicleRouteDTO> vehiclePaths = new ArrayList<>();
            List<RealPathDTO> geoRoutes = new ArrayList<>();
            List<AssignedOrderDTO> assignedOrders = new ArrayList<>();

            //IMPORTANTE: è normale che ci sia un FOR qui?????
            for (Map.Entry<String, List<RescueAssignmentDTO>> e : perVehicle.entrySet()) {
                String vehId = e.getKey();
                VehicleDTO vdto = vehicleById.get(vehId);
                if (vdto == null) {
                    resp.setCode(RouteResultDTO.INVALID_REQUEST);
                    resp.setMessage("VehicleDTO non trovato: " + vehId);
                    return resp;
                }

                // 1) Se IN_TRANSIT prova a RIUSARE la route attiva; altrimenti crea nuova
                Optional<VehicleRoute> maybeActive =
                        vehicleRouteRepository.findFirstByVehicleIdAndCompletedFalseOrderByIdDesc(vehId);

                VehicleRoute vr;
                List<RouteNodeDTO> routeNodes;
                int oldSize;

                if (vdto.getStatus() == VehicleStatus.IN_TRANSIT && maybeActive.isPresent()) {
                    vr = maybeActive.get();
                    routeNodes = new ArrayList<>(vr.getRoute() != null ? vr.getRoute() : List.of());

                    // se la route finisce al DEPOT, rimuovi l'ultimo nodo per inserire le tappe di soccorso prima del rientro
                    if (!routeNodes.isEmpty()
                            && depotNode.getId().equals(routeNodes.get(routeNodes.size() - 1).getNodeIndex())) {
                        routeNodes.remove(routeNodes.size() - 1);
                    }
                    oldSize = routeNodes.size();
                } else {
                    vr = new VehicleRoute();
                    vr.setVehicleId(vehId);
                    vr.setVehicleStatus(vdto.getStatus());
                    vr.setAssociatedUserId(null);   //messo a null
                    vr.setCompleted(false);
                    vr.setCurrentSegmentIndex(0); //!!! SE IN TRANSIT FA RICOMINCIARE DA CAPO L'INDEX DELLE TAPPE

                    // start virtuale: posizione corrente se IN_TRANSIT, altrimenti DEPOT
                    double startLat = (vdto.getStatus() == VehicleStatus.IN_TRANSIT && vdto.getCurrentLat() != null)
                            ? vdto.getCurrentLat() : depotNode.getLat();
                    double startLon = (vdto.getStatus() == VehicleStatus.IN_TRANSIT && vdto.getCurrentLon() != null)
                            ? vdto.getCurrentLon() : depotNode.getLon();

                    routeNodes = new ArrayList<>();
                    RouteNodeDTO startNode = new RouteNodeDTO();
                    startNode.setNodeIndex(startIndex(vehId)); // "START_<vehId>"
                    startNode.setLat(startLat);
                    startNode.setLon(startLon);
                    routeNodes.add(startNode);

                    oldSize = routeNodes.size();
                }

                // 2) Append delle tappe di soccorso (pickup/delivery)
                List<String> routeOrderIds = new ArrayList<>();
                boolean insertedInMiddle = false;

                for (RescueAssignmentDTO a : e.getValue()) {
                    OrderDTO o = orderById.get(a.getOrderId());

                    AssignedOrderDTO ao = new AssignedOrderDTO();
                    ao.setOrderId(a.getOrderId());
                    ao.setAssignedVehicleId(vehId);
                    if (o != null) {
                        ao.setPickupNodeId(o.getPickupNodeId());
                        ao.setDeliveryNodeId(o.getDeliveryNodeId());
                    }
                    assignedOrders.add(ao);

                    boolean isPickedRescue = "PICKED_UP_RESCUE".equals(a.getMode());

                    if (vdto.getStatus() == VehicleStatus.IN_TRANSIT && maybeActive.isPresent() && isPickedRescue) {
                        // V1.5: inserzione [handover→delivery] dopo currentSegmentIndex
                        int lockIdx = vr.getCurrentSegmentIndex();
                        String handoverIdx = "RESCUE_" + a.getOrderId();

                        Insertion ins = bestPairInsertion(
                                routeNodes,
                                a.getPickupLat(), a.getPickupLon(),     // handover = anomaly
                                a.getDeliveryLat(), a.getDeliveryLon(), // delivery reale
                                lockIdx
                        );

                        if (ins != null) {
                            applyPairInsertion(routeNodes, ins,
                                    handoverIdx, a.getPickupLat(), a.getPickupLon(),
                                    o.getDeliveryNodeId(), a.getDeliveryLat(), a.getDeliveryLon());
                            insertedInMiddle = true;
                        } else {
                            // fallback: append in coda
                            RouteNodeDTO p = new RouteNodeDTO();
                            p.setNodeIndex(handoverIdx); p.setLat(a.getPickupLat()); p.setLon(a.getPickupLon());
                            routeNodes.add(p);
                            RouteNodeDTO d = new RouteNodeDTO();
                            d.setNodeIndex(o.getDeliveryNodeId()); d.setLat(a.getDeliveryLat()); d.setLon(a.getDeliveryLon());
                            routeNodes.add(d);
                        }

                    } else {
                        // AVAILABLE (o altro caso): semplice append pickup→delivery
                        RouteNodeDTO p = new RouteNodeDTO();
                        p.setNodeIndex(isPickedRescue ? ("RESCUE_" + a.getOrderId()) : o.getPickupNodeId());
                        p.setLat(a.getPickupLat()); p.setLon(a.getPickupLon());
                        routeNodes.add(p);

                        RouteNodeDTO d = new RouteNodeDTO();
                        d.setNodeIndex(o.getDeliveryNodeId());
                        d.setLat(a.getDeliveryLat()); d.setLon(a.getDeliveryLon());
                        routeNodes.add(d);
                    }

                    routeOrderIds.add(a.getOrderId());
                }

                // 3) Chiudi al DEPOT (se non è già l’ultimo nodo)
                if (routeNodes.isEmpty() || !depotNode.getId().equals(routeNodes.get(routeNodes.size() - 1).getNodeIndex())) {
                    RouteNodeDTO depotEnd = new RouteNodeDTO();
                    depotEnd.setNodeIndex(depotNode.getId());
                    depotEnd.setLat(depotNode.getLat());
                    depotEnd.setLon(depotNode.getLon());
                    routeNodes.add(depotEnd);
                }

                int lockIdx = (vdto.getStatus() == VehicleStatus.IN_TRANSIT && maybeActive.isPresent())
                        ? vr.getCurrentSegmentIndex()
                        : -1; // nessun lock per AVAILABLE
                routeNodes = dedupConsecutiveFrom(routeNodes, lockIdx);

                // 4) Salva la route (update/insert)
                vr.setRoute(routeNodes);
                try {
                    vehicleRouteRepository.save(vr);
                } catch (Exception ex) {
                    resp.setCode(RouteResultDTO.PERSISTENCE_ERROR);
                    resp.setMessage("Errore salvataggio VehicleRoute: " + ex.getMessage());
                    return resp;
                }

                // 5) VehicleRouteDTO per payload
                VehicleRouteDTO vPath = new VehicleRouteDTO();
                vPath.setId(vr.getId());
                vPath.setVehicleId(vehId);
                vPath.setRoute(routeNodes);
                vPath.setVehicleStatus(vdto.getStatus());
                vPath.setCompleted(false);
                vPath.setAssociatedUserId(
                        (vdto.getStatus() == VehicleStatus.IN_TRANSIT && maybeActive.isPresent())
                                ? vr.getAssociatedUserId()    // preserva l'assegnato sulla route attiva
                                : null                        // route nuova (AVAILABLE) → nessun camionista
                );
                vPath.setCurrentSegmentIndex(vr.getCurrentSegmentIndex());    ////!!! SE IN TRANSIT FA RICOMINCIARE DA CAPO L'INDEX DELLE TAPPE
                vPath.setOrderIds(routeOrderIds);
                vehiclePaths.add(vPath);

                // --- Mappe semantiche per QUESTO veicolo (coprono TUTTE le sue tappe) ---
                List<OrderDTO> active = orderRepository
                        .findByAssignedVehicleIdAndStatusIn(
                                vehId,
                                List.of(OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS, OrderStatus.PICKED_UP)
                        )
                        .stream().map(orderService::toDTO).toList();

                Set<String> routeNodeIdsUsed = routeNodes.stream()
                        .map(RouteNodeDTO::getNodeIndex).collect(Collectors.toSet());

                active = active.stream().filter(o ->
                        routeNodeIdsUsed.contains(o.getPickupNodeId()) ||
                                routeNodeIdsUsed.contains(o.getDeliveryNodeId())
                ).toList();

                Map<String, List<String>> pickupByNodeAny   = new HashMap<>();
                Map<String, List<String>> deliveryByNodeAny = new HashMap<>();
                for (OrderDTO o : active) {
                    pickupByNodeAny.computeIfAbsent(o.getPickupNodeId(), k -> new ArrayList<>()).add(o.getId());
                    deliveryByNodeAny.computeIfAbsent(o.getDeliveryNodeId(), k -> new ArrayList<>()).add(o.getId());
                }

                // 6) RealPath SOLO per gli archi nuovi; skippa archi 0m
                if (insertedInMiddle) {
                    realPathRepository.deleteByRouteId(vr.getId());
                    for (int i = 0; i < routeNodes.size() - 1; i++) {
                        RouteNodeDTO from = routeNodes.get(i);
                        RouteNodeDTO to   = routeNodes.get(i + 1);
                        if (Double.compare(from.getLat(), to.getLat()) == 0 &&
                                Double.compare(from.getLon(), to.getLon()) == 0) continue;

                        List<List<Double>> geometry; double distM, timeS;
                        try {
                            geometry = graphhopperClient.getRoute(from.getLat(), from.getLon(), to.getLat(), to.getLon());
                            distM = graphhopperClient.getDistance();
                            timeS = graphhopperClient.getTime();
                        } catch (Exception ex) {
                            resp.setCode(RouteResultDTO.GRAPH_ERROR);
                            resp.setMessage("Graphhopper error su "+from.getNodeIndex()+"→"+to.getNodeIndex()+": "+ex.getMessage());
                            return resp;
                        }

                        List<String> edgeOrderIds = new ArrayList<>();
                        String toIdx = to.getNodeIndex();
                        if (toIdx.startsWith("RESCUE_")) {
                            edgeOrderIds.add(toIdx.substring("RESCUE_".length()));
                        } else {
                            if (pickupByNodeAny.containsKey(toIdx))   edgeOrderIds.addAll(pickupByNodeAny.get(toIdx));
                            if (deliveryByNodeAny.containsKey(toIdx)) edgeOrderIds.addAll(deliveryByNodeAny.get(toIdx));
                        }
                        edgeOrderIds = edgeOrderIds.stream()
                                .distinct()
                                .collect(Collectors.toCollection(ArrayList::new));

                        if (Objects.equals(toIdx, depotNode.getId())) {
                            edgeOrderIds = List.of(); // niente clear()
                        }


                        RealPath rp = new RealPath();
                        rp.setFromNodeIndex(from.getNodeIndex());
                        rp.setToNodeIndex(to.getNodeIndex());
                        rp.setFromLabel(semanticLabel(from.getNodeIndex(), depotNode, pickupByNodeAny, deliveryByNodeAny, nodeById));
                        rp.setToLabel(  semanticLabel(to.getNodeIndex(),   depotNode, pickupByNodeAny, deliveryByNodeAny, nodeById));
                        rp.setGeometry(geometry);
                        rp.setDistanceM(distM);
                        rp.setTimeS(timeS);
                        rp.setVehicleId(vehId);
                        rp.setRouteId(vr.getId());
                        rp.setOrderIds(edgeOrderIds);
                        realPathRepository.save(rp);

                        RealPathDTO rpd = new RealPathDTO();
                        rpd.setId(rp.getId());
                        rpd.setFromNodeIndex(rp.getFromNodeIndex());
                        rpd.setToNodeIndex(rp.getToNodeIndex());
                        rpd.setFromLabel(rp.getFromLabel());
                        rpd.setToLabel(rp.getToLabel());
                        rpd.setGeometry(rp.getGeometry());
                        rpd.setDistanceM(rp.getDistanceM());
                        rpd.setTimeS(rp.getTimeS());
                        rpd.setVehicleId(rp.getVehicleId());
                        rpd.setRouteId(rp.getRouteId());
                        rpd.setOrderIds(rp.getOrderIds());
                        geoRoutes.add(rpd);
                    }
                } else {
                    int fromIdx = Math.max(0, oldSize - 1);
                    for (int i = fromIdx; i < routeNodes.size() - 1; i++) {
                        RouteNodeDTO from = routeNodes.get(i);
                        RouteNodeDTO to   = routeNodes.get(i + 1);
                        if (Double.compare(from.getLat(), to.getLat()) == 0 &&
                                Double.compare(from.getLon(), to.getLon()) == 0) continue;

                        List<List<Double>> geometry; double distM, timeS;
                        try {
                            geometry = graphhopperClient.getRoute(from.getLat(), from.getLon(), to.getLat(), to.getLon());
                            distM = graphhopperClient.getDistance();
                            timeS = graphhopperClient.getTime();
                        } catch (Exception ex) {
                            resp.setCode(RouteResultDTO.GRAPH_ERROR);
                            resp.setMessage("Graphhopper error su "+from.getNodeIndex()+"→"+to.getNodeIndex()+": "+ex.getMessage());
                            return resp;
                        }

                        List<String> edgeOrderIds = new ArrayList<>();
                        String toIdx = to.getNodeIndex();
                        if (toIdx.startsWith("RESCUE_")) {
                            edgeOrderIds.add(toIdx.substring("RESCUE_".length()));
                        } else {
                            if (pickupByNodeAny.containsKey(toIdx))   edgeOrderIds.addAll(pickupByNodeAny.get(toIdx));
                            if (deliveryByNodeAny.containsKey(toIdx)) edgeOrderIds.addAll(deliveryByNodeAny.get(toIdx));
                        }
                        edgeOrderIds = edgeOrderIds.stream()
                                .distinct()
                                .collect(Collectors.toCollection(ArrayList::new));

                        if (Objects.equals(toIdx, depotNode.getId())) {
                            edgeOrderIds = List.of(); // niente clear()
                        }

                        RealPath rp = new RealPath();
                        rp.setFromNodeIndex(from.getNodeIndex());
                        rp.setToNodeIndex(to.getNodeIndex());
                        rp.setFromLabel(semanticLabel(from.getNodeIndex(), depotNode, pickupByNodeAny, deliveryByNodeAny, nodeById));
                        rp.setToLabel(  semanticLabel(to.getNodeIndex(),   depotNode, pickupByNodeAny, deliveryByNodeAny, nodeById));
                        rp.setGeometry(geometry);
                        rp.setDistanceM(distM);
                        rp.setTimeS(timeS);
                        rp.setVehicleId(vehId);
                        rp.setRouteId(vr.getId());
                        rp.setOrderIds(edgeOrderIds);
                        realPathRepository.save(rp);

                        RealPathDTO rpd = new RealPathDTO();
                        rpd.setId(rp.getId());
                        rpd.setFromNodeIndex(rp.getFromNodeIndex());
                        rpd.setToNodeIndex(rp.getToNodeIndex());
                        rpd.setFromLabel(rp.getFromLabel());
                        rpd.setToLabel(rp.getToLabel());
                        rpd.setGeometry(rp.getGeometry());
                        rpd.setDistanceM(rp.getDistanceM());
                        rpd.setTimeS(rp.getTimeS());
                        rpd.setVehicleId(rp.getVehicleId());
                        rpd.setRouteId(rp.getRouteId());
                        rpd.setOrderIds(rp.getOrderIds());
                        geoRoutes.add(rpd);
                    }
                }

                // 7) MQTT: invia solo se il veicolo ha già un driver
                if (vdto.getAssignedUserId() != null && !vdto.getAssignedUserId().isBlank()) {
                    try{
                        mqttPublisher.publishRouteStarted(vehId, vr.getId(), "rescue");
                        log.info("ANOMALY: phase B - mqtt publish done");
                    } catch (Exception ex) {
                        log.error("MQTT publish failed", ex);
                        throw ex; // per capire se è qui
                    }

                }
            }

            try{
                SolutionDTO solution = new SolutionDTO();
                solution.setPath(vehiclePaths);
                solution.setAssignedOrders(assignedOrders);

                OptimizerResultDTO data = new OptimizerResultDTO();
                data.setSolution(solution);
                data.setGeoRoutes(geoRoutes);
                data.setCode(RouteResultDTO.OK);
                data.setMessage("Riassegnazione GREEDY completata: " + plan.getAssignments().size() + " ordini");

                resp.setCode(RouteResultDTO.OK);
                resp.setMessage(data.getMessage());
                resp.setData(data);

                log.info("ANOMALY: phase C - response built OK");
                return resp;
            }catch (Exception ex){
                log.error("ANOMALY: building response failed", ex);
                throw ex;
            }
// 5) monta OptimizerResultDTO → resp.data

        } catch (Exception e) {
            log.error("handleAnomalyGreedy FAILED", e); // stacktrace in log
            resp.setCode(RouteResultDTO.ERROR);
            resp.setMessage("Errore durante la gestione dell'anomalia: "
                    + e.getClass().getSimpleName()
                    + (e.getMessage() != null ? (" - " + e.getMessage()) : ""));
            return resp;
        }
    }

    //helper vari
    private static String startIndex(String vehicleId) {
        return "START_" + vehicleId;
    }
    private static String labelForNodeIndex(String nodeIndex, Map<String, NodeDTO> nodeById) {
        if (nodeIndex == null) return "";
        if (nodeIndex.startsWith("START_"))  return "Start";
        if (nodeIndex.startsWith("RESCUE_")) return "Rescue " + nodeIndex.substring("RESCUE_".length());
        NodeDTO n = nodeById.get(nodeIndex);
        return (n != null && n.getName() != null) ? n.getName() : nodeIndex;
    }

    private static String semanticLabel(
            String nodeIndex,
            NodeDTO depotNode,
            Map<String, ?> pickupByNodeAny,
            Map<String, ?> deliveryByNodeAny,
            Map<String, NodeDTO> nodeById
    ) {
        if (nodeIndex == null) return "";
        if (nodeIndex.startsWith("START_"))  return "Start";
        if (nodeIndex.startsWith("RESCUE_")) return "Pickup"; // handover trattato come pickup
        if (Objects.equals(nodeIndex, depotNode.getId())) return "Depot";
        if (pickupByNodeAny.containsKey(nodeIndex))   return "Pickup";
        if (deliveryByNodeAny.containsKey(nodeIndex)) return "Delivery";
        NodeDTO n = nodeById.get(nodeIndex);
        return (n != null && n.getName() != null) ? n.getName() : nodeIndex;
    }


    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0, dLat = Math.toRadians(lat2-lat1), dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2*R*Math.asin(Math.sqrt(a));
    }

    private static record Insertion(int iAfter, int jAfter, double deltaKm) {}

    private Insertion bestPairInsertion(List<RouteNodeDTO> r,
                                        double aLat, double aLon,  // handover
                                        double bLat, double bLon,  // delivery
                                        int lockIdx) {             // non tocchiamo <= lockIdx
        if (r == null || r.size() < 2) return null;
        double best = Double.POSITIVE_INFINITY; Insertion bestIns = null;
        int start = Math.max(lockIdx, 0);
        for (int i = start; i < r.size()-1; i++) {
            for (int j = i+1; j < r.size(); j++) {
                RouteNodeDTO ri  = r.get(i), rip = r.get(Math.min(i+1, r.size()-1));
                RouteNodeDTO rj  = r.get(j), rjp = r.get(Math.min(j+1, r.size()-1));
                double base = haversineKm(ri.getLat(),ri.getLon(), rip.getLat(),rip.getLon()) +
                        haversineKm(rj.getLat(),rj.getLon(), rjp.getLat(),rjp.getLon());
                double with = haversineKm(ri.getLat(),ri.getLon(), aLat,aLon) +
                        haversineKm(aLat,aLon, rip.getLat(),rip.getLon()) +
                        haversineKm(rj.getLat(),rj.getLon(), bLat,bLon) +
                        haversineKm(bLat,bLon, rjp.getLat(),rjp.getLon());
                double delta = with - base;
                if (delta < best) { best = delta; bestIns = new Insertion(i, j, delta); }
            }
        }
        return bestIns;
    }

    private void applyPairInsertion(List<RouteNodeDTO> r,
                                    Insertion ins,
                                    String handoverIdx, double aLat, double aLon,
                                    String deliveryIdx, double bLat, double bLon) {
        RouteNodeDTO A = new RouteNodeDTO(); A.setNodeIndex(handoverIdx); A.setLat(aLat); A.setLon(aLon);
        RouteNodeDTO B = new RouteNodeDTO(); B.setNodeIndex(deliveryIdx); B.setLat(bLat); B.setLon(bLon);
        r.add(ins.iAfter()+1, A);
        r.add(ins.jAfter()+2, B); // +1 perché A ha già spostato gli indici
    }

    private List<RouteNodeDTO> dedupConsecutiveFrom(List<RouteNodeDTO> in, int lockIdx) {
        if (in == null || in.size() < 2) return in;
        List<RouteNodeDTO> out = new ArrayList<>(in.size());
        for (int i = 0; i < in.size(); i++) {
            RouteNodeDTO cur = in.get(i);
            if (out.isEmpty()) { out.add(cur); continue; }

            // non toccare il prefisso <= lockIdx (per veicoli IN_TRANSIT)
            if (i <= lockIdx) { out.add(cur); continue; }

            RouteNodeDTO prev = out.get(out.size() - 1);
            boolean sameIndex = Objects.equals(prev.getNodeIndex(), cur.getNodeIndex());
            boolean sameLatLon = Double.compare(prev.getLat(), cur.getLat()) == 0 &&
                    Double.compare(prev.getLon(), cur.getLon()) == 0;

            if (sameIndex && sameLatLon) continue; // drop duplicato consecutivo
            out.add(cur);
        }
        return out;
    }

}
