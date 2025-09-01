package it.unisalento.pas2425.deliveryserviceproject.dto;


public class NodeResultDTO {

    public static final int OK = 200;
    public static final int NODE_NOT_FOUND = 401;
    public static final int MISSING_DATA = 402;
    public static final int NODE_ALREADY_EXIST = 403;
    public static final int ERROR_TYPE_OF_NODE = 404;
    public static final int INVALID_NODE_FORMAT = 405;

    private int result;
    private String message;
    private NodeDTO node;

    // Getter & Setter

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

    public NodeDTO getNode() {
        return node;
    }

    public void setNode(NodeDTO node) {
        this.node = node;
    }

}