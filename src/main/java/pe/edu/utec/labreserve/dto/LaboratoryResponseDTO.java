package pe.edu.utec.labreserve.dto;

import pe.edu.utec.labreserve.entity.Laboratory;

public record LaboratoryResponseDTO(Long id, String name, String location, Long managerId, String status) {

    public static LaboratoryResponseDTO from(Laboratory laboratory) {
        return new LaboratoryResponseDTO(
                laboratory.getId(),
                laboratory.getName(),
                laboratory.getLocation(),
                laboratory.getManagerId(),
                laboratory.getStatus().name()
        );
    }
}
