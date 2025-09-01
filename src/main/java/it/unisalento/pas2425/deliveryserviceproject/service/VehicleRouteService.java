package it.unisalento.pas2425.deliveryserviceproject.service;

import it.unisalento.pas2425.deliveryserviceproject.components.MqttPublisher;
import it.unisalento.pas2425.deliveryserviceproject.components.NotificationClient;
import it.unisalento.pas2425.deliveryserviceproject.components.PositionClient;
import it.unisalento.pas2425.deliveryserviceproject.domain.*;
import it.unisalento.pas2425.deliveryserviceproject.dto.*;
import it.unisalento.pas2425.deliveryserviceproject.mapper.RealPathMapper;
import it.unisalento.pas2425.deliveryserviceproject.repositories.OrderRepository;
import it.unisalento.pas2425.deliveryserviceproject.repositories.RealPathRepository;
import it.unisalento.pas2425.deliveryserviceproject.repositories.VehicleRepository;
import it.unisalento.pas2425.deliveryserviceproject.repositories.VehicleRouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VehicleRouteService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleRouteRepository vehicleRouteRepository;

    @Autowired
    private PositionClient positionClient;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RealPathRepository realPathRepository;

    @Autowired
    private MqttPublisher mqttPublisher;

    public AssignResultDTO assignDriver(String userId, VehicleRouteDTO routeDTO, String jwtToken) {
        AssignResultDTO result = new AssignResultDTO();

        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(routeDTO.getVehicleId());
        if (vehicleOpt.isEmpty()) {
            result.setCode(AssignResultDTO.VEHICLE_NOT_FOUND);
            result.setMessage("Veicolo non trovato");
            return result;
        }

        Vehicle vehicle = vehicleOpt.get();
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            result.setCode(AssignResultDTO.VEHICLE_NOT_AVAILABLE);
            result.setMessage("Veicolo non disponibile per l'assegnazione");
            return result;
        }
        vehicle.setAssignedUserId(userId);
        vehicle.setStatus(VehicleStatus.ASSIGNED);
        vehicleRepository.save(vehicle);
        Optional<VehicleRoute> routeOpt = vehicleRouteRepository.findById(routeDTO.getId());
        if (routeOpt.isPresent()) {
            VehicleRoute route = routeOpt.get();
            route.setAssociatedUserId(userId); // ✅ salvi l’assegnazione sulla tratta
            vehicleRouteRepository.save(route);
            mqttPublisher.publishRouteAssigned(vehicle.getId(), route.getId());
        }

        UserResultDTO positionResult = positionClient.updateUserStatus(userId, UserStatus.ASSIGNED, jwtToken);
        if (positionResult.getCode() == UserResultDTO.OK) {
            result.setCode(AssignResultDTO.OK);
            result.setMessage("Utente assegnato e veicolo aggiornato con successo");
        } else {
            result.setCode(AssignResultDTO.VEHICLE_OK_USER_ERROR);
            result.setMessage("Veicolo aggiornato, ma errore nel cambio stato utente: " + positionResult.getMessage());
        }

        return result;
    }

    public List<VehicleRouteDTO> getAllRoutes() {
        return vehicleRouteRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<VehicleRouteDTO> getActiveRoutes() {
        List<Vehicle> vehicles = vehicleRepository.findAll();

        // Mappa veicoloId → Vehicle
        Map<String, Vehicle> vehicleMap = vehicles.stream()
                .collect(Collectors.toMap(Vehicle::getId, v -> v));

        return vehicleRouteRepository.findAll()
                .stream()
                .filter(route ->
                        route.getRoute() != null &&
                                route.getRoute().size() > 2 &&
                                vehicleMap.containsKey(route.getVehicleId()) &&
                                vehicleMap.get(route.getVehicleId()).getStatus() == VehicleStatus.AVAILABLE
                )
                .map(this::toDTO)
                .toList();
    }

    public Optional<VehicleRouteDTO> getRouteById(String routeId) {
        return vehicleRouteRepository.findById(routeId)
                .map(this::toDTO);
    }

    public VehicleRouteListDTO getRoutesByUserId(String userId) {
        List<VehicleRoute> allRoutes = vehicleRouteRepository.findAllByAssociatedUserId(userId);

        List<VehicleRouteDTO> dtos = allRoutes.stream()
                .map(route -> {
                    Vehicle vehicle = vehicleRepository.findById(route.getVehicleId()).orElse(null);
                    return toDTO(route, vehicle);
                })
                .toList();

        VehicleRouteListDTO listDTO = new VehicleRouteListDTO();
        listDTO.setRoutes(dtos);
        return listDTO;
    }

    private VehicleRouteDTO toDTO(VehicleRoute entity) {
        VehicleRouteDTO dto = new VehicleRouteDTO();
        dto.setId(entity.getId());
        dto.setVehicleId(entity.getVehicleId());
        dto.setRoute(entity.getRoute());
        dto.setCompleted(entity.isCompleted());
        dto.setAssociatedUserId(entity.getAssociatedUserId());
        dto.setCurrentSegmentIndex(entity.getCurrentSegmentIndex());
        dto.setVehicleStatus(entity.getVehicleStatus());
        return dto;
    }

    // Versione estesa con Vehicle
    private VehicleRouteDTO toDTO(VehicleRoute entity, Vehicle vehicle) {
        VehicleRouteDTO dto = toDTO(entity); // riusa la prima
        dto.setVehicleStatus(vehicle.getStatus()); // imposta lo stato

        return dto;
    }

    public AcceptRouteResultDTO acceptRoute(String routeId, String jwtToken) {
        Optional<VehicleRoute> routeOpt = vehicleRouteRepository.findById(routeId);
        if (routeOpt.isEmpty()) {
            return new AcceptRouteResultDTO(AcceptRouteResultDTO.ROUTE_NOT_FOUND, "Tratta non trovata");
        }

        VehicleRoute route = routeOpt.get();

        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(route.getVehicleId());
        if (vehicleOpt.isEmpty()) {
            return new AcceptRouteResultDTO(AcceptRouteResultDTO.VEHICLE_NOT_FOUND, "Veicolo non trovato");
        }

        Vehicle vehicle = vehicleOpt.get();

        if (vehicle.getAssignedUserId() == null || vehicle.getAssignedUserId().isBlank()) {
            return new AcceptRouteResultDTO(AcceptRouteResultDTO.VEHICLE_NO_USER_ASSIGNED, "Veicolo non ha un utente assegnato");
        }

        vehicle.setStatus(VehicleStatus.IN_TRANSIT);
        vehicleRepository.save(vehicle);

        route.setCurrentSegmentIndex(0); // Prima tappa
        route.setVehicleStatus(VehicleStatus.IN_TRANSIT);
        vehicleRouteRepository.save(route);

        UserResultDTO positionResult = positionClient.updateUserStatus(vehicle.getAssignedUserId(), UserStatus.ON_ROUTE, jwtToken);
        if (positionResult.getCode() != UserResultDTO.OK) {
            return new AcceptRouteResultDTO(AcceptRouteResultDTO.USER_UPDATE_FAILED, "Errore nell'aggiornamento dello stato dell'utente: " + positionResult.getMessage());
        }

        List<Order> orders = orderRepository.findByVehicleRouteId(routeId);
        for (Order order : orders) {
            order.setStatus(OrderStatus.IN_PROGRESS);
        }
        orderRepository.saveAll(orders);

        mqttPublisher.publishRouteStarted(vehicle.getId(), routeId, "normal");

        UserDTO assignedUser;
        try {
            assignedUser = positionClient.getUserById(vehicle.getAssignedUserId(), jwtToken);
            System.out.println("Utente accettato tratta: " + assignedUser.getName() + " " + assignedUser.getSurname());
        } catch (Exception e) {
            return new AcceptRouteResultDTO(AcceptRouteResultDTO.USER_FETCH_FAILED, "Errore nel recupero dell’utente: " + e.getMessage());
        }

        String adminPushToken = positionClient.getPushTokenByUserId(jwtToken); // ← adesso non serve più passare l'id
        if (adminPushToken == null || adminPushToken.isBlank()) {
            return new AcceptRouteResultDTO(AcceptRouteResultDTO.ADMIN_TOKEN_NOT_FOUND, "Token di notifica admin non trovato");
        }


        NotificationRequestDTO req = new NotificationRequestDTO();
        req.setTo(adminPushToken);
        req.setTitle("Tratta accettata");
        req.setBody("Il camionista " + assignedUser.getName() + " " + assignedUser.getSurname()
                + " ha accettato la tratta " + route.getId());

        NotificationResponseDTO notifRes = notificationClient.sendNotification(req);
        if (notifRes.getCode() != NotificationResponseDTO.OK) {
            return new AcceptRouteResultDTO(AcceptRouteResultDTO.NOTIFICATION_FAILED, "Errore notifica admin: " + notifRes.getMessage());
        }

        return new AcceptRouteResultDTO(AcceptRouteResultDTO.OK, "Ordine accettato correttamente");
    }

    public List<RealPathDTO> getRealPathByRouteId(String routeId) {
        List<RealPath> paths = realPathRepository.findByRouteId(routeId);
        return RealPathMapper.toRealPathDTO(paths);
    }

    public RouteResultDTO updateProgress(String routeId, UpdateProgressDTO dto) {
        RouteResultDTO result = new RouteResultDTO();

        VehicleRoute route = vehicleRouteRepository.findById(routeId).orElse(null);
        if (route == null) {
            result.setCode(RouteResultDTO.ERROR);
            result.setMessage("Route non trovata");
            return result;
        }

        route.setCurrentSegmentIndex(dto.getCurrentSegmentIndex());
        vehicleRouteRepository.save(route);

        result.setCode(RouteResultDTO.OK);
        result.setMessage("Progresso aggiornato correttamente");
        return result;
    }

    public VehicleRouteDTO getActiveRoute(String userId) {
        Optional<VehicleRoute> routeOpt = vehicleRouteRepository.findByAssociatedUserIdAndVehicleStatus(userId, VehicleStatus.IN_TRANSIT);

        return routeOpt.map(this::toDTO).orElse(null);
    }

    public RouteResultDTO deleteRoute(String routeId) {
        RouteResultDTO result = new RouteResultDTO();
        Optional<VehicleRoute> routeOpt = vehicleRouteRepository.findById(routeId);

        if (routeOpt.isPresent()) {
            vehicleRouteRepository.deleteById(routeId);
            result.setCode(RouteResultDTO.OK);
            result.setMessage("Route eliminata con successo.");
        } else {
            result.setCode(RouteResultDTO.ERROR);
            result.setMessage("Route non trovata.");
        }
        return result;
    }
}

