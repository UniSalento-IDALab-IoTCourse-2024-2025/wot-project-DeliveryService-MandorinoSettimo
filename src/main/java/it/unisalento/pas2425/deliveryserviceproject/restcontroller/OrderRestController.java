package it.unisalento.pas2425.deliveryserviceproject.restcontroller;

import it.unisalento.pas2425.deliveryserviceproject.domain.OrderStatus;
import it.unisalento.pas2425.deliveryserviceproject.dto.OrderDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.OrderListDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.OrderResultDTO;
import it.unisalento.pas2425.deliveryserviceproject.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderRestController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResultDTO> addOrder(@RequestBody OrderDTO dto) {
        OrderResultDTO result = orderService.addOrder(dto);
        return result.getResult() == OrderResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @GetMapping
    public ResponseEntity<OrderListDTO> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResultDTO> deleteOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.deleteOrder(orderId));
    }

    @PatchMapping("/{orderId}/assign/{vehicleId}")
    public ResponseEntity<OrderResultDTO> assignVehicle(
            @PathVariable String orderId,
            @PathVariable String vehicleId
    ) {
        return ResponseEntity.ok(orderService.assignVehicle(orderId, vehicleId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResultDTO> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam OrderStatus status
    ) {
        OrderResultDTO result = orderService.updateOrderStatus(orderId, status);
        return result.getResult() == OrderResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    // Ottieni tutti gli ordini con uno specifico stato
    @GetMapping("/status/{status}")
    public ResponseEntity<OrderListDTO> getOrdersByStatus(@PathVariable String status) {
        try {
            OrderListDTO orders = orderService.getOrdersByStatus(status);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build(); // se status non è valido
        }
    }

    @PatchMapping("/{orderId}/mark-delivered")
    public ResponseEntity<OrderResultDTO> markOrderDelivered(@PathVariable String orderId) {
        OrderResultDTO result = orderService.markOrderDelivered(orderId);
        return result.getResult() == OrderResultDTO.OK
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

}
