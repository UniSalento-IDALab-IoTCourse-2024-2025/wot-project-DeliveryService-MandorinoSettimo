package it.unisalento.pas2425.deliveryserviceproject.mapper;

import it.unisalento.pas2425.deliveryserviceproject.domain.RealPath;
import it.unisalento.pas2425.deliveryserviceproject.dto.RealPathDTO;

import java.util.List;
import java.util.stream.Collectors;

public class RealPathMapper {
    public static List<RealPath> fromDTOList(List<RealPathDTO> dtos) {
        return dtos.stream().map(dto -> {
            RealPath path = new RealPath();
            path.setVehicleId(dto.getVehicleId());
            path.setFromNodeIndex(dto.getFromNodeIndex()); // 👈 Usa direttamente, è già una String
            path.setToNodeIndex(dto.getToNodeIndex());     // 👈 Idem
            path.setFromLabel(dto.getFromLabel());
            path.setToLabel(dto.getToLabel());
            path.setDistanceM(dto.getDistanceM());
            path.setTimeS(dto.getTimeS());
            path.setGeometry(dto.getGeometry());
            path.setRouteId(dto.getRouteId());
            path.setOrderIds(dto.getOrderIds());
            return path;
        }).collect(Collectors.toList());
    }

    public static List<RealPathDTO> toRealPathDTO(List<RealPath> paths) {
        return paths.stream().map(path -> {
            RealPathDTO dto = new RealPathDTO();
            dto.setId(path.getId());
            dto.setFromNodeIndex(path.getFromNodeIndex());
            dto.setToNodeIndex(path.getToNodeIndex());
            dto.setFromLabel(path.getFromLabel());
            dto.setToLabel(path.getToLabel());
            dto.setVehicleId(path.getVehicleId());
            dto.setDistanceM(path.getDistanceM());
            dto.setTimeS(path.getTimeS());
            dto.setGeometry(path.getGeometry());
            dto.setRouteId(path.getRouteId());
            dto.setOrderIds(path.getOrderIds());
            return dto;
        }).toList();
    }

}
