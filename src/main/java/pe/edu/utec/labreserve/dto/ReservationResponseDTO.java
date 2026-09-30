package pe.edu.utec.labreserve.dto;

import pe.edu.utec.labreserve.entity.LabReservation;

public record ReservationResponseDTO(Long id, Long slotId, String studentUsername, String status) {

    public static ReservationResponseDTO from(LabReservation reservation) {
        return new ReservationResponseDTO(
                reservation.getId(),
                reservation.getSlotId(),
                reservation.getStudent().getUsername(),
                reservation.getStatus().name()
        );
    }
}
