package it.unisalento.pas2425.deliveryserviceproject.dto;

import java.util.List;

public class GreedyRescueResultDTO {
    public static final int OK = 200;
    public static final int INVALID_REQUEST = 400;
    public static final int NO_ORDERS = 401;
    public static final int NO_CANDIDATES = 402;
    public static final int NO_SOLUTION = 403;


    private List<RescueAssignmentDTO> assignments;
    private String message;
    private int code;

    public GreedyRescueResultDTO(List<RescueAssignmentDTO> assignments, String message, int code) {
        this.assignments = assignments;
        this.message = message;
        this.code = code;
    }

    public List<RescueAssignmentDTO> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<RescueAssignmentDTO> assignments) {
        this.assignments = assignments;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
