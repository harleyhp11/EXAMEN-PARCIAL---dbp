package pe.edu.utec.labreserve.service;

import pe.edu.utec.labreserve.dto.CreateReservationRequestDTO;
import pe.edu.utec.labreserve.dto.MyReservationResponseDTO;
import pe.edu.utec.labreserve.dto.PageResponseDTO;
import pe.edu.utec.labreserve.dto.ReservationResponseDTO;
import pe.edu.utec.labreserve.entity.EquipmentSlot;
import pe.edu.utec.labreserve.entity.LabReservation;
import pe.edu.utec.labreserve.entity.ReservationStatus;
import pe.edu.utec.labreserve.entity.SlotStatus;
import pe.edu.utec.labreserve.entity.User;
import pe.edu.utec.labreserve.exception.BusinessRuleException;
import pe.edu.utec.labreserve.exception.InvalidRequestException;
import pe.edu.utec.labreserve.exception.ResourceNotFoundException;
import pe.edu.utec.labreserve.repository.EquipmentSlotRepository;
import pe.edu.utec.labreserve.repository.LabReservationRepository;
import pe.edu.utec.labreserve.repository.UserRepository;
import pe.edu.utec.labreserve.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Locale;

@Service
public class ReservationService {

    private final LabReservationRepository reservationRepository;
    private final EquipmentSlotRepository slotRepository;
    private final UserRepository userRepository;

    public ReservationService(LabReservationRepository reservationRepository,
                              EquipmentSlotRepository slotRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReservationResponseDTO reserve(Long slotId,
                                          CreateReservationRequestDTO request,
                                          AppUserPrincipal actor) {

        EquipmentSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> ResourceNotFoundException.of("Turno", slotId));

        if (slot.getStatus() == SlotStatus.CANCELLED) {
            throw new BusinessRuleException("El turno esta cancelado");
        }

        if (reservationRepository.existsForSlotAndStudent(slotId, actor.getId())) {
            throw new BusinessRuleException("Ya tiene una reserva para este turno");
        }

        if (hasOverlappingReservation(actor.getId(), slot.getStartTime(), slot.getEndTime())) {
            throw new BusinessRuleException("Ya tiene una reserva que se solapa con ese horario");
        }

        long reserved = reservationRepository.countBySlotAndStatus(slotId, ReservationStatus.RESERVED);
        if (reserved >= slot.getCapacity()) {
            throw new BusinessRuleException("El turno ya no tiene capacidad disponible");
        }

        User student = userRepository.findById(actor.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", actor.getId()));

        LabReservation reservation = new LabReservation(
                slot, student, request.purpose(), ZonedDateTime.now());
        LabReservation saved = reservationRepository.save(reservation);

        if (reserved + 1 >= slot.getCapacity()) {
            slot.setStatus(SlotStatus.FULL);
            slotRepository.save(slot);
        }

        return ReservationResponseDTO.from(saved);
    }

    public boolean hasOverlappingReservation(Long studentId, ZonedDateTime start, ZonedDateTime end) {
        return reservationRepository.countOverlappingForStudent(
                studentId, start, end, ReservationStatus.RESERVED) > 0;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<MyReservationResponseDTO> findMine(AppUserPrincipal actor,
                                                              String status,
                                                              int page,
                                                              int size) {
        Page<LabReservation> result = reservationRepository.findMine(
                actor.getId(), parseStatus(status), PageRequest.of(page, size));
        return PageResponseDTO.from(result, MyReservationResponseDTO::from);
    }

    private ReservationStatus parseStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("all")) {
            return null;
        }
        try {
            return ReservationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("status debe ser all, reserved, used o cancelled");
        }
    }
}
