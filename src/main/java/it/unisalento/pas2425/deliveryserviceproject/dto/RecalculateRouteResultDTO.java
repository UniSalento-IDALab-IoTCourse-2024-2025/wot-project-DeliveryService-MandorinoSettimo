package it.unisalento.pas2425.deliveryserviceproject.dto;

public class RecalculateRouteResultDTO {
    public static final int OK = 200;
    public static final int SEGMENT_NOT_FOUND = 404;
    public static final int DESTINATION_NODE_NOT_FOUND = 405;
    public static final int GRAPHOPPER_ERROR = 500;

    private RealPathDTO updatedSegment;
    private int code;
    private String message;

    public RealPathDTO getUpdatedSegment() {
        return updatedSegment;
    }

    public void setUpdatedSegment(RealPathDTO updatedSegment) {
        this.updatedSegment = updatedSegment;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
