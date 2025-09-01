package it.unisalento.pas2425.deliveryserviceproject.restcontroller;

import it.unisalento.pas2425.deliveryserviceproject.domain.VehicleStatus;
import it.unisalento.pas2425.deliveryserviceproject.dto.VehicleDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.VehicleListDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.VehicleResultDTO;
import it.unisalento.pas2425.deliveryserviceproject.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleRestController {

    @Autowired
    private VehicleService vehicleService;

    // Aggiungi veicolo
    @PostMapping
    public ResponseEntity<VehicleResultDTO> addVehicle(@RequestBody VehicleDTO vehicleDTO) {
        VehicleResultDTO result = vehicleService.addVehicle(vehicleDTO);

        return result.getResult() == VehicleResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    // Tutti i veicoli
    @GetMapping
    public ResponseEntity<VehicleListDTO> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    // Ottieni veicolo per ID
    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleDTO> getVehicleById(@PathVariable String vehicleId) {
        return vehicleService.getVehicleById(vehicleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Modifica veicolo
    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleResultDTO> updateVehicle(
            @PathVariable String vehicleId,
            @RequestBody VehicleDTO vehicleDTO
    ) {
        VehicleResultDTO result = vehicleService.updateVehicle(vehicleId, vehicleDTO);
        return result.getResult() == VehicleResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    // Elimina veicolo
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<VehicleResultDTO> deleteVehicle(@PathVariable String vehicleId) {
        VehicleResultDTO result = vehicleService.deleteVehicle(vehicleId);
        return result.getResult() == VehicleResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    // Ottieni tutti i veicoli con uno specifico stato
    @GetMapping("/status/{status}")
    public ResponseEntity<VehicleListDTO> getVehiclesByStatus(@PathVariable String status) {
        VehicleListDTO vehicles = vehicleService.getVehiclesByStatus(status.toUpperCase());
        return ResponseEntity.ok(vehicles);
    }

    // Aggiorna lo stato di un veicolo
    @PatchMapping("/{vehicleId}/status")
    public ResponseEntity<VehicleResultDTO> updateVehicleStatus(
            @PathVariable String vehicleId,
            @RequestParam String status
    ) {
        VehicleResultDTO result = vehicleService.updateVehicleStatus(vehicleId, status.toUpperCase());
        return result.getResult() == VehicleResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

}
