package pe.edu.utec.labreserve.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import pe.edu.utec.labreserve.dto.CreateReservationRequestDTO;
import pe.edu.utec.labreserve.dto.MyReservationResponseDTO;
import pe.edu.utec.labreserve.dto.PageResponseDTO;
import pe.edu.utec.labreserve.dto.ReservationResponseDTO;
import pe.edu.utec.labreserve.security.AppUserPrincipal;
import pe.edu.utec.labreserve.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/equipment-slots/{slotId}/reservations")
    public ResponseEntity<ReservationResponseDTO> reserve(
            @PathVariable Long slotId,
            @Valid @RequestBody CreateReservationRequestDTO request,
            @AuthenticationPrincipal AppUserPrincipal actor) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.reserve(slotId, request, actor));
    }

    @GetMapping("/my-lab-reservations")
    public ResponseEntity<PageResponseDTO<MyReservationResponseDTO>> findMine(
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @AuthenticationPrincipal AppUserPrincipal actor) {

        return ResponseEntity.ok(reservationService.findMine(actor, status, page, size));
    }
}
