package pe.edu.utec.labreserve.service;

import pe.edu.utec.labreserve.dto.CreateSlotRequestDTO;
import pe.edu.utec.labreserve.dto.PageResponseDTO;
import pe.edu.utec.labreserve.dto.SlotResponseDTO;
import pe.edu.utec.labreserve.entity.EquipmentSlot;
import pe.edu.utec.labreserve.entity.Laboratory;
import pe.edu.utec.labreserve.entity.LaboratoryStatus;
import pe.edu.utec.labreserve.entity.Role;
import pe.edu.utec.labreserve.entity.SlotStatus;
import pe.edu.utec.labreserve.exception.BusinessRuleException;
import pe.edu.utec.labreserve.exception.InvalidRequestException;
import pe.edu.utec.labreserve.exception.ResourceNotFoundException;
import pe.edu.utec.labreserve.repository.EquipmentSlotRepository;
import pe.edu.utec.labreserve.repository.LaboratoryRepository;
import pe.edu.utec.labreserve.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
public class EquipmentSlotService {

    private final EquipmentSlotRepository slotRepository;
    private final LaboratoryRepository laboratoryRepository;

    public EquipmentSlotService(EquipmentSlotRepository slotRepository,
                                LaboratoryRepository laboratoryRepository) {
        this.slotRepository = slotRepository;
        this.laboratoryRepository = laboratoryRepository;
    }

    @Transactional
    public SlotResponseDTO createSlot(Long laboratoryId, CreateSlotRequestDTO request, AppUserPrincipal actor) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Laboratorio", laboratoryId));

        if (laboratory.getStatus() != LaboratoryStatus.ACTIVE) {
            throw new BusinessRuleException("El laboratorio no esta ACTIVE");
        }

        if (actor.getRole() == Role.ROLE_TECHNICIAN && !actor.getId().equals(laboratory.getManagerId())) {
            throw new AccessDeniedException("El tecnico no gestiona este laboratorio");
        }

        if (!request.startTime().isBefore(request.endTime())) {
            throw new InvalidRequestException("startTime debe ser anterior a endTime");
        }
        if (!request.startTime().isAfter(ZonedDateTime.now())) {
            throw new InvalidRequestException("startTime debe ser una fecha futura");
        }

        boolean overlaps = hasOverlap(laboratoryId, request.equipmentCode(), request.startTime(), request.endTime());
        if (overlaps) {
            throw new BusinessRuleException("El equipo ya tiene un turno que se solapa con ese horario");
        }

        EquipmentSlot slot = new EquipmentSlot(
                laboratory,
                request.equipmentCode(),
                request.startTime(),
                request.endTime(),
                request.capacity()
        );
        return SlotResponseDTO.from(slotRepository.save(slot));
    }

    public boolean hasOverlap(Long laboratoryId, String equipmentCode, ZonedDateTime start, ZonedDateTime end) {
        List<EquipmentSlot> existing = slotRepository.findActiveByLaboratoryAndEquipment(
                laboratoryId, equipmentCode, SlotStatus.CANCELLED);
        return existing.stream().anyMatch(slot -> slot.overlapsWith(start, end));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<SlotResponseDTO> search(Long laboratoryId,
                                                   String equipmentCode,
                                                   ZonedDateTime from,
                                                   int page,
                                                   int size) {
        String codeFilter = (equipmentCode == null || equipmentCode.isBlank()) ? null : equipmentCode.trim();

        Page<EquipmentSlot> result = slotRepository.search(
                laboratoryId,
                codeFilter,
                from,
                ZonedDateTime.now(),
                SlotStatus.AVAILABLE,
                PageRequest.of(page, size)
        );
        return PageResponseDTO.from(result, SlotResponseDTO::from);
    }
}
