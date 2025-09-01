// Service per la gestione degli ordini
package it.unisalento.pas2425.deliveryserviceproject.service;

import it.unisalento.pas2425.deliveryserviceproject.domain.Order;
import it.unisalento.pas2425.deliveryserviceproject.domain.OrderStatus;
import it.unisalento.pas2425.deliveryserviceproject.dto.OrderDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.OrderListDTO;
import it.unisalento.pas2425.deliveryserviceproject.dto.OrderResultDTO;
import it.unisalento.pas2425.deliveryserviceproject.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Aggiunge un nuovo ordine al sistema.

     */
    public OrderResultDTO addOrder(OrderDTO dto) {
        OrderResultDTO result = new OrderResultDTO();

        // Validazione dei campi obbligatori
        if (dto.getPickupNodeId() == null || dto.getPickupNodeId().isBlank() ||
                dto.getDeliveryNodeId() == null || dto.getDeliveryNodeId().isBlank() ||
                dto.getTimeWindow() == null || dto.getQuantity() <= 0) {

            result.setResult(OrderResultDTO.INVALID_ORDER);
            result.setMessage("Dati ordine mancanti o non validi");
            return result;
        }

        if (dto.getPickupNodeId().equals(dto.getDeliveryNodeId())) {
            result.setResult(OrderResultDTO.INVALID_ORDER);
            result.setMessage("Pickup e delivery node coincidono");
            return result;
        }

        // Creazione ordine
        Order order = new Order();
        order.setPickupNodeId(dto.getPickupNodeId());
        order.setDeliveryNodeId(dto.getDeliveryNodeId());
        order.setQuantity(dto.getQuantity());
        order.setTwOpen((int) Instant.now().getEpochSecond());
        order.setTwClose(order.getTwOpen() + dto.getTimeWindow().getSeconds());
        order.setTimeWindow(dto.getTimeWindow());
        order.setStatus(OrderStatus.PENDING);

        order = orderRepository.save(order);

        result.setResult(OrderResultDTO.OK);
        result.setMessage("Ordine salvato con successo");
        result.setOrder(toDTO(order));
        return result;
    }


    /**
     * Recupera un ordine per ID.

     */
    public OrderListDTO getAllOrders() {
        long now = Instant.now().getEpochSecond();

        List<Order> allOrders = orderRepository.findAll();

        // Controlla e aggiorna gli ordini scaduti
        boolean changesMade = false;
        for (Order order : allOrders) {
            if ((order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.ASSIGNED)
                    && order.getTwClose() < now) {
                order.setStatus(OrderStatus.FAILED);
                changesMade = true;
            }

        }

        // Salva solo se ci sono modifiche
        if (changesMade) {
            orderRepository.saveAll(allOrders);
        }

        // Converte in DTO
        List<OrderDTO> orders = allOrders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        OrderListDTO list = new OrderListDTO();
        list.setOrders(orders);
        return list;
    }

    public OrderListDTO getOrdersByStatus(String status) {
        OrderStatus statusEnum = OrderStatus.valueOf(status.toUpperCase());

        List<OrderDTO> orders = orderRepository.findByStatus(statusEnum)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        OrderListDTO list = new OrderListDTO();
        list.setOrders(orders);
        return list;
    }



    /**
     * Recupera un ordine per ID.
     *
     */
    public OrderResultDTO getOrderById(String id) {
        OrderResultDTO result = new OrderResultDTO();
        Optional<Order> optional = orderRepository.findById(id);

        if (optional.isEmpty()) {
            result.setResult(OrderResultDTO.ORDER_NOT_FOUND);
            result.setMessage("Ordine non trovato");
            return result;
        }

        Order order = optional.get();
        result.setResult(OrderResultDTO.OK);
        result.setMessage("Ordine recuperato con successo");
        result.setOrder(toDTO(order));
        return result;
    }

    /**
     * Elimina un ordine per ID.
     *
     */
    public OrderResultDTO deleteOrder(String id) {
        OrderResultDTO result = new OrderResultDTO();
        if (!orderRepository.existsById(id)) {
            result.setResult(OrderResultDTO.ORDER_NOT_FOUND);
            result.setMessage("Ordine non trovato");
            return result;
        }

        orderRepository.deleteById(id);
        result.setResult(OrderResultDTO.OK);
        result.setMessage("Ordine eliminato con successo");
        return result;
    }

    /**
     * Aggiorna lo stato di un ordine.
     * da capire se è da far gestire al camionista o al sistema
     */
    public OrderResultDTO updateOrderStatus(String orderId, OrderStatus status) {
        OrderResultDTO result = new OrderResultDTO();
        Optional<Order> optional = orderRepository.findById(orderId);

        if (optional.isEmpty()) {
            result.setResult(OrderResultDTO.ORDER_NOT_FOUND);
            result.setMessage("Ordine non trovato");
            return result;
        }

        Order order = optional.get();
        order.setStatus(status);
        orderRepository.save(order);

        result.setResult(OrderResultDTO.OK);
        result.setMessage("Stato dell'ordine aggiornato");
        result.setOrder(toDTO(order));
        return result;
    }

    public void expirePendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        long now = Instant.now().getEpochSecond(); // oppure System.currentTimeMillis() / 1000

        for (Order order : pendingOrders) {
            if (order.getTwClose() < now) {
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
            }
        }
    }

    /**
     * Assegna un veicolo a un ordine.
     *
     */
    public OrderResultDTO assignVehicle(String orderId, String vehicleId) {
        OrderResultDTO result = new OrderResultDTO();
        Optional<Order> optional = orderRepository.findById(orderId);

        if (optional.isEmpty()) {
            result.setResult(OrderResultDTO.ORDER_NOT_FOUND);
            result.setMessage("Ordine non trovato");
            return result;
        }

        Order order = optional.get();
        order.setAssignedVehicleId(vehicleId);
        order = orderRepository.save(order);

        result.setResult(OrderResultDTO.OK);
        result.setMessage("Veicolo assegnato all'ordine");
        result.setOrder(toDTO(order));
        return result;
    }

    /**
     * Segna un ordine come consegnato.
     *
     */
    public OrderResultDTO markOrderDelivered(String orderId) {
        OrderResultDTO result = new OrderResultDTO();
        Optional<Order> optional = orderRepository.findById(orderId);

        if (optional.isEmpty()) {
            result.setResult(OrderResultDTO.ORDER_NOT_FOUND);
            result.setMessage("Ordine non trovato");
            return result;
        }

        Order order = optional.get();
        order.setStatus(OrderStatus.DELIVERED);
        //todo (opzionale): si potrebbe vedere se è in ritardo oppure no
        // magari utili da sapere all'admin: e aggiungere STATUS: DELIVERED_BUT_LATE
        orderRepository.save(order);

        result.setResult(OrderResultDTO.OK);
        result.setMessage("Ordine consegnato con successo");
        result.setOrder(toDTO(order));
        return result;
    }



    public OrderDTO toDTO(Order o) {
        OrderDTO dto = new OrderDTO();
        dto.setId(o.getId());
        dto.setPickupNodeId(o.getPickupNodeId());
        dto.setDeliveryNodeId(o.getDeliveryNodeId());
        dto.setQuantity(o.getQuantity());
        dto.setTwOpen(o.getTwOpen());
        dto.setTwClose(o.getTwClose());
        dto.setTimeWindow(o.getTimeWindow());
        dto.setStatus(o.getStatus());
        if (o.getAssignedVehicleId() != null) {
            dto.setAssignedVehicleId(o.getAssignedVehicleId());
        }
        //qua l'ho già aggiunto
        if (o.getVehicleRouteId() != null) {
            dto.setVehicleRouteId(o.getVehicleRouteId());
        }
        return dto;
    }
}
