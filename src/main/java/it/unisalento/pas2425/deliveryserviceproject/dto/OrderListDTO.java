package it.unisalento.pas2425.deliveryserviceproject.dto;

import java.util.List;

public class OrderListDTO {

    private List<OrderDTO> orders;

    public OrderListDTO() {}

    public OrderListDTO(List<OrderDTO> orders) {
        this.orders = orders;
    }

    public List<OrderDTO> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderDTO> orders) {
        this.orders = orders;
    }
}
