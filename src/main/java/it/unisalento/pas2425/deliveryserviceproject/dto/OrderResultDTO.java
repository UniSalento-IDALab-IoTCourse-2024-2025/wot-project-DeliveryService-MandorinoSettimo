package it.unisalento.pas2425.deliveryserviceproject.dto;

public class OrderResultDTO {

    public static final int OK = 200;
    public static final int ORDER_NOT_FOUND = 401;
    public static final int INVALID_ORDER = 402;
    public static final int VEHICLE_ASSIGN_FAILED = 403;

    private int result;
    private String message;
    private OrderDTO order;

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OrderDTO getOrder() {
        return order;
    }

    public void setOrder(OrderDTO order) {
        this.order = order;
    }
}
