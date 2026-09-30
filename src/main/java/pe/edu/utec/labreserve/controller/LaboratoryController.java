package pe.edu.utec.labreserve.controller;

import jakarta.validation.Valid;
import pe.edu.utec.labreserve.dto.CreateLaboratoryRequestDTO;
import pe.edu.utec.labreserve.dto.LaboratoryResponseDTO;
import pe.edu.utec.labreserve.service.LaboratoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/laboratories")
public class LaboratoryController {

    private final LaboratoryService laboratoryService;

    public LaboratoryController(LaboratoryService laboratoryService) {
        this.laboratoryService = laboratoryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LaboratoryResponseDTO> create(@Valid @RequestBody CreateLaboratoryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(laboratoryService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<LaboratoryResponseDTO>> findAll() {
        return ResponseEntity.ok(laboratoryService.findAll());
    }
}
