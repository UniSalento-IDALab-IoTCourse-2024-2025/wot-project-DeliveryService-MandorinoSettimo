package it.unisalento.pas2425.deliveryserviceproject.restcontroller;

import it.unisalento.pas2425.deliveryserviceproject.dto.*;
import it.unisalento.pas2425.deliveryserviceproject.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
public class RouteRestController {

    @Autowired
    private RouteService routeService;

    @PostMapping("/optimize")
    public ResponseEntity<RouteResultDTO> optimizeFromJava(@RequestBody OrderListDTO dto, @RequestHeader("Authorization") String token) {

        OptimizerResultDTO result = routeService.optimize(dto, token);
        RouteResultDTO response = new RouteResultDTO();

        if (result.getCode() != OptimizerResultDTO.OK) {
            response.setCode(RouteResultDTO.ERROR);
            response.setMessage("Errore nell'ottimizzazione: " + result.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        response.setCode(RouteResultDTO.OK);
        response.setMessage("Ottimizzazione completata");
        response.setData(result);

        return ResponseEntity.ok(response);

    }

    @PostMapping("/{routeId}/complete")
    public ResponseEntity<RouteResultDTO> completeRoute(@PathVariable String routeId, @RequestHeader("Authorization") String token) {
            RouteResultDTO result = routeService.completeRoute(routeId, token);
            return result.getCode() == RouteResultDTO.OK
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/recalculate-route")
    public ResponseEntity<RecalculateRouteResultDTO> recalculateRoute(@RequestBody RecalculateRouteRequestDTO request, @RequestHeader("Authorization") String token) {

        RecalculateRouteResultDTO result = routeService.recalculateRoute(request, token);
        return result.getCode() == RouteResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/report-anomaly")
    public ResponseEntity<RouteResultDTO> reportAnomaly(@RequestBody ReportAnomalyRequestDTO request, @RequestHeader("Authorization") String token) {
        RouteResultDTO response = routeService.handleAnomalyGreedy(request, token);
        return response.getCode() == RouteResultDTO.OK
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);

    }


}
