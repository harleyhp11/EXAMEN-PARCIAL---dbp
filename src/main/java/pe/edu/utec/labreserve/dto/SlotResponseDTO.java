package pe.edu.utec.labreserve.dto;

import pe.edu.utec.labreserve.entity.EquipmentSlot;

public record SlotResponseDTO(Long id, String laboratoryName, String equipmentCode, String status) {

    public static SlotResponseDTO from(EquipmentSlot slot) {
        return new SlotResponseDTO(
                slot.getId(),
                slot.getLaboratory().getName(),
                slot.getEquipmentCode(),
                slot.getStatus().name()
        );
    }
}
