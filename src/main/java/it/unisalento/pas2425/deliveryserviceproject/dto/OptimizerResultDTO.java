package it.unisalento.pas2425.deliveryserviceproject.dto;

import java.util.List;

public class OptimizerResultDTO {

    public final static int OK = 200;
    public static final int ERROR = 500;
    public static final int MISSING_DATA = 400;
    public static final int NO_AVAILABLE_VEHICLES = 401;
    public static final int MISSING_DEPOT = 402;
    public static final int NOT_ENOUGH_USERS = 403;
    public static final int NO_SOLUTION = 404;

    private SolutionDTO solution;
    private List<RealPathDTO> geoRoutes;
    private int code;
    private String message;


    public SolutionDTO getSolution() {
        return solution;
    }

    public void setSolution(SolutionDTO solution) {
        this.solution = solution;
    }

    public List<RealPathDTO> getGeoRoutes() {
        return geoRoutes;
    }

    public void setGeoRoutes(List<RealPathDTO> geoRoutes) {
        this.geoRoutes = geoRoutes;
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
