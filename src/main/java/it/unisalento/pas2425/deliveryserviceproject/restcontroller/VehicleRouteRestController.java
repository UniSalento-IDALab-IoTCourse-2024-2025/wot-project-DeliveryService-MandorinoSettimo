package it.unisalento.pas2425.deliveryserviceproject.restcontroller;

import it.unisalento.pas2425.deliveryserviceproject.domain.Vehicle;
import it.unisalento.pas2425.deliveryserviceproject.domain.VehicleRoute;
import it.unisalento.pas2425.deliveryserviceproject.dto.*;
import it.unisalento.pas2425.deliveryserviceproject.repositories.VehicleRepository;
import it.unisalento.pas2425.deliveryserviceproject.repositories.VehicleRouteRepository;
import it.unisalento.pas2425.deliveryserviceproject.service.VehicleRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vehicle-routes")
public class VehicleRouteRestController {


    @Autowired
    private VehicleRouteRepository vehicleRouteRepository;

    @Autowired
    private VehicleRouteService vehicleRouteService;


    @GetMapping
    public ResponseEntity<VehicleRouteListDTO> getAllRoutes() {
        VehicleRouteListDTO response = new VehicleRouteListDTO();
        response.setRoutes(vehicleRouteService.getAllRoutes());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<VehicleRouteListDTO> getActiveRoutes() {
        VehicleRouteListDTO response = new VehicleRouteListDTO();
        response.setRoutes(vehicleRouteService.getActiveRoutes());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<VehicleRouteDTO> getRouteById(@PathVariable String routeId) {
        Optional<VehicleRouteDTO> routeDTO = vehicleRouteService.getRouteById(routeId);
        return routeDTO.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<VehicleRouteDTO> getRouteByVehicleId(@PathVariable String vehicleId) {
        return vehicleRouteRepository.findByVehicleId(vehicleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/assign-driver/{userId}")
    public ResponseEntity<AssignResultDTO> assignDriver(@PathVariable String userId, @RequestBody VehicleRouteDTO routeDTO, @RequestHeader("Authorization") String jwtToken) {

        AssignResultDTO response = vehicleRouteService.assignDriver(userId, routeDTO, jwtToken);
        return response.getCode() == AssignResultDTO.OK
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/from/{userId}")
    public ResponseEntity<VehicleRouteListDTO> getRoutesByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(vehicleRouteService.getRoutesByUserId(userId));
    }

    @PostMapping("/accept/{routeId}")
    public ResponseEntity<AcceptRouteResultDTO> acceptRoute(@PathVariable String routeId, @RequestHeader("Authorization") String jwtToken) {
        AcceptRouteResultDTO result = vehicleRouteService.acceptRoute(routeId, jwtToken);
        return result.getCode() == AcceptRouteResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/{routeId}/realpath")
    public ResponseEntity<List<RealPathDTO>> getRealPathForRoute(@PathVariable String routeId) {
        List<RealPathDTO> paths = vehicleRouteService.getRealPathByRouteId(routeId);
        return ResponseEntity.ok(paths);
    }

    @PatchMapping("/{routeId}/update-progress")
    public ResponseEntity<RouteResultDTO> updateProgress(@PathVariable String routeId, @RequestBody UpdateProgressDTO dto) {
        RouteResultDTO result = vehicleRouteService.updateProgress(routeId, dto);
        return result.getCode() == RouteResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/from/{userId}/active")
    public ResponseEntity<VehicleRouteDTO> getActiveRoute(@PathVariable String userId) {
        VehicleRouteDTO routeDTO = vehicleRouteService.getActiveRoute(userId);

        if (routeDTO != null) {
            return ResponseEntity.ok(routeDTO);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<RouteResultDTO> deleteRoute(@PathVariable String routeId) {
        RouteResultDTO result = vehicleRouteService.deleteRoute(routeId);
        return result.getCode() == RouteResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

}
