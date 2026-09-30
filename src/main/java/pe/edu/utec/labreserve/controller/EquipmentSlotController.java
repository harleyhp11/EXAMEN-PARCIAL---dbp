package pe.edu.utec.labreserve.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import pe.edu.utec.labreserve.dto.CreateSlotRequestDTO;
import pe.edu.utec.labreserve.dto.PageResponseDTO;
import pe.edu.utec.labreserve.dto.SlotResponseDTO;
import pe.edu.utec.labreserve.security.AppUserPrincipal;
import pe.edu.utec.labreserve.service.EquipmentSlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
@Validated
public class EquipmentSlotController {

    private final EquipmentSlotService slotService;

    public EquipmentSlotController(EquipmentSlotService slotService) {
        this.slotService = slotService;
    }

    @PostMapping("/laboratories/{labId}/slots")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<SlotResponseDTO> createSlot(@PathVariable Long labId,
                                                      @Valid @RequestBody CreateSlotRequestDTO request,
                                                      @AuthenticationPrincipal AppUserPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(slotService.createSlot(labId, request, actor));
    }

    @GetMapping("/equipment-slots")
    public ResponseEntity<PageResponseDTO<SlotResponseDTO>> search(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) String equipmentCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        return ResponseEntity.ok(slotService.search(laboratoryId, equipmentCode, from, page, size));
    }
}
