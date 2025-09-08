package it.unisalento.pas2425.deliveryserviceproject.service;

import it.unisalento.pas2425.deliveryserviceproject.domain.Order;
import it.unisalento.pas2425.deliveryserviceproject.domain.Vehicle;
import it.unisalento.pas2425.deliveryserviceproject.domain.VehicleStatus;
import it.unisalento.pas2425.deliveryserviceproject.dto.VehicleDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.VehicleListDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.VehicleResultDTO;
import it.unisalento.pas2425.deliveryserviceproject.repositories.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    /**
     * Aggiunge un veicolo al sistema.

     */
    public VehicleResultDTO addVehicle(VehicleDTO dto) {
        VehicleResultDTO result = new VehicleResultDTO();

        if (dto.getPlate() == null || dto.getPlate().isBlank()) {
            result.setResult(VehicleResultDTO.MISSING_DATA);
            result.setMessage("Targa veicolo mancante");
            return result;
        }

        dto.setPlate(dto.getPlate().toUpperCase());

        if (!dto.getPlate().matches("^[A-Z]{2}[0-9]{3}[A-Z]{2}$")) {
            result.setResult(VehicleResultDTO.INVALID_PLATE_FORMAT);
            result.setMessage("Formato targa non valido. Esempio: AB123CD");
            return result;
        }

        if (vehicleRepository.findByPlate(dto.getPlate()).isPresent()) {
            result.setResult(VehicleResultDTO.VEHICLE_ALREADY_EXIST);
            result.setMessage("Veicolo con la stessa targa già esistente");
            return result;
        }

        if (dto.getCapacity() <= 0) {
            result.setResult(VehicleResultDTO.MISSING_DATA);
            result.setMessage("Capacità del veicolo mancante o non valida");
            return result;
        }

        if (dto.getStatus() == null) {
            dto.setStatus(VehicleStatus.AVAILABLE);
        }

        dto.setCost(10);
        dto.setCurrentLat(null);
        dto.setCurrentLon(null);
        dto.setAssignedUserId(null);
        Vehicle vehicle = fromDTO(dto);
        vehicle = vehicleRepository.save(vehicle);

        result.setResult(VehicleResultDTO.OK);
        result.setMessage("Veicolo aggiunto con successo");
        result.setVehicle(toDTO(vehicle));
        return result;
    }

    /**
     * Recupera tutti i veicoli registrati nel sistema.

     */
    public VehicleListDTO getAllVehicles() {
        List<VehicleDTO> vehicles = vehicleRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        VehicleListDTO vehicleListDTO = new VehicleListDTO();
        vehicleListDTO.setVehicles(vehicles);
        return vehicleListDTO;
    }

    /**
     * Recupera un veicolo in base al suo ID.

     */
    public Optional<VehicleDTO> getVehicleById(String vehicleId) {
        return vehicleRepository.findById(vehicleId).map(this::toDTO);
    }

    /**
     * Recupera i veicoli in base al loro stato.

     */
    public VehicleListDTO getVehiclesByStatus(String status) {
        List<VehicleDTO> vehicles = vehicleRepository.findByStatus(status)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        VehicleListDTO vehicleListDTO = new VehicleListDTO();
        vehicleListDTO.setVehicles(vehicles);
        return vehicleListDTO;
    }


    /**
     * Aggiorna le informazioni di un veicolo esistente.

     */
    public VehicleResultDTO updateVehicle(String vehicleId, VehicleDTO dto) {
        VehicleResultDTO result = new VehicleResultDTO();
        Optional<Vehicle> optional = vehicleRepository.findById(vehicleId);

        if (optional.isEmpty()) {
            result.setResult(VehicleResultDTO.VEHICLE_NOT_FOUND);
            result.setMessage("Veicolo non trovato");
            return result;
        }

        Vehicle vehicle = optional.get();

        if (dto.getPlate() != null) vehicle.setPlate(dto.getPlate());
        if (dto.getCapacity() > 0) vehicle.setCapacity(dto.getCapacity());
        if (dto.getCost() > 0) vehicle.setCost(dto.getCost());
        if (dto.getCurrentLat() != 0) vehicle.setCurrentLat(dto.getCurrentLat());
        if (dto.getCurrentLon() != 0) vehicle.setCurrentLon(dto.getCurrentLon());
        if (dto.getStatus() != null) vehicle.setStatus(dto.getStatus());
        if (dto.getAssignedUserId() != null) vehicle.setAssignedUserId(dto.getAssignedUserId());
        if (dto.getLastHeadingDeg() != null) vehicle.setLastHeadingDeg(dto.getLastHeadingDeg());
        if (dto.getLastSpeedKmh() != null) vehicle.setLastSpeedKmh(dto.getLastSpeedKmh());
        if (dto.getLastPositionAt() != null) vehicle.setLastPositionAt(dto.getLastPositionAt());

        vehicle = vehicleRepository.save(vehicle);

        result.setResult(VehicleResultDTO.OK);
        result.setMessage("Veicolo aggiornato con successo");
        result.setVehicle(toDTO(vehicle));
        return result;
    }

    /**
     * Elimina un veicolo dal sistema.

     */
    public VehicleResultDTO deleteVehicle(String vehicleId) {
        VehicleResultDTO result = new VehicleResultDTO();

        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            result.setResult(VehicleResultDTO.VEHICLE_NOT_FOUND);
            result.setMessage("Veicolo non trovato");
            return result;
        }

        Vehicle vehicle = vehicleOpt.get();
        if (vehicle.getStatus() == VehicleStatus.IN_TRANSIT) {
            result.setResult(VehicleResultDTO.VEHICLE_IN_TRANSIT);
            result.setMessage("Impossibile eliminare il veicolo: è attualmente in transito");
            return result;
        }

        vehicleRepository.deleteById(vehicleId);
        result.setResult(VehicleResultDTO.OK);
        result.setMessage("Veicolo eliminato con successo");
        return result;
    }



    /**
     * Aggiorna lo stato di un veicolo.

     */
    public VehicleResultDTO updateVehicleStatus(String vehicleId, String status) {
        VehicleResultDTO result = new VehicleResultDTO();
        Optional<Vehicle> optional = vehicleRepository.findById(vehicleId);

        if (optional.isEmpty()) {
            result.setResult(VehicleResultDTO.VEHICLE_NOT_FOUND);
            result.setMessage("Veicolo non trovato");
            return result;
        }

        Vehicle vehicle = optional.get();
        vehicle.setStatus(VehicleStatus.valueOf(status));
        vehicle.setAssignedUserId(null);
        vehicleRepository.save(vehicle);

        result.setResult(VehicleResultDTO.OK);
        result.setMessage("Stato aggiornato con successo");
        result.setVehicle(toDTO(vehicle));
        return result;
    }

    public List<Vehicle> findAvailableOrCapableVehicles(double anomalyLat, double anomalyLon, List<Order> ordersToRecover) {
        int totalQuantity = ordersToRecover.stream()
                .mapToInt(Order::getQuantity)
                .sum();

        List<Vehicle> all = vehicleRepository.findAll();

        return all.stream()
                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE || v.getStatus() == VehicleStatus.IN_TRANSIT)
                .filter(v -> v.getCapacity() >= totalQuantity)
                .sorted(Comparator.comparingDouble(v -> distance(anomalyLat, anomalyLon, v.getCurrentLat(), v.getCurrentLon())))
                .collect(Collectors.toList());
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        // Distanza Euclidea per semplicità (oppure Haversine se vuoi precisione)
        return Math.sqrt(Math.pow(lat1 - lat2, 2) + Math.pow(lon1 - lon2, 2));
    }

    public VehicleDTO toDTO(Vehicle v) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(v.getId());
        dto.setPlate(v.getPlate());
        dto.setCapacity(v.getCapacity());
        dto.setCost(v.getCost());
        dto.setCurrentLat(v.getCurrentLat());
        dto.setCurrentLon(v.getCurrentLon());
        dto.setStatus(v.getStatus());
        dto.setAssignedUserId(v.getAssignedUserId());
        return dto;
    }

    private Vehicle fromDTO(VehicleDTO dto) {
        Vehicle v = new Vehicle();
        v.setPlate(dto.getPlate());
        v.setCapacity(dto.getCapacity());
        v.setCost(dto.getCost());
        v.setStatus(dto.getStatus());
        if (dto.getCurrentLat() != null) v.setCurrentLat(dto.getCurrentLat());
        if (dto.getCurrentLon() != null) v.setCurrentLon(dto.getCurrentLon());
        if (dto.getAssignedUserId() != null) v.setAssignedUserId(dto.getAssignedUserId());
        if (dto.getLastHeadingDeg() != null) v.setLastHeadingDeg(dto.getLastHeadingDeg());
        if (dto.getLastSpeedKmh() != null) v.setLastSpeedKmh(dto.getLastSpeedKmh());
        if (dto.getLastPositionAt() != null) v.setLastPositionAt(dto.getLastPositionAt());
        return v;
    }

}
