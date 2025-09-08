package it.unisalento.pas2425.deliveryserviceproject.restcontroller;

import it.unisalento.pas2425.deliveryserviceproject.dto.LiveVehicleDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.VehicleContextDTO;
import it.unisalento.pas2425.deliveryserviceproject.service.AdminLiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicles")
public class AdminLiveRestController {

    @Autowired
    private AdminLiveService adminLiveService;

    @GetMapping("/live")
    public ResponseEntity<List<LiveVehicleDTO>> live(@RequestHeader("Authorization") String jwtToken) {
        return ResponseEntity.ok(adminLiveService.getLiveVehicles(jwtToken));
    }

    @GetMapping("/{vehicleId}/context")
    public ResponseEntity<VehicleContextDTO> context(@PathVariable String vehicleId) {
        VehicleContextDTO dto = adminLiveService.getVehicleContext(vehicleId);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }
}
