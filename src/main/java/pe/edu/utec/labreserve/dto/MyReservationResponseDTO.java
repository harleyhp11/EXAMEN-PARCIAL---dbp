package pe.edu.utec.labreserve.dto;

import pe.edu.utec.labreserve.entity.LabReservation;

public record MyReservationResponseDTO(Long id, String equipmentCode, String status) {

    public static MyReservationResponseDTO from(LabReservation reservation) {
        return new MyReservationResponseDTO(
                reservation.getId(),
                reservation.getSlot().getEquipmentCode(),
                reservation.getStatus().name()
        );
    }
}
