package pe.edu.utec.labreserve.service;

import pe.edu.utec.labreserve.dto.CreateReservationRequestDTO;
import pe.edu.utec.labreserve.dto.ReservationResponseDTO;
import pe.edu.utec.labreserve.entity.EquipmentSlot;
import pe.edu.utec.labreserve.entity.LabReservation;
import pe.edu.utec.labreserve.entity.Laboratory;
import pe.edu.utec.labreserve.entity.LaboratoryStatus;
import pe.edu.utec.labreserve.entity.ReservationStatus;
import pe.edu.utec.labreserve.entity.Role;
import pe.edu.utec.labreserve.entity.SlotStatus;
import pe.edu.utec.labreserve.entity.User;
import pe.edu.utec.labreserve.exception.BusinessRuleException;
import pe.edu.utec.labreserve.exception.ResourceNotFoundException;
import pe.edu.utec.labreserve.repository.EquipmentSlotRepository;
import pe.edu.utec.labreserve.repository.LabReservationRepository;
import pe.edu.utec.labreserve.repository.UserRepository;
import pe.edu.utec.labreserve.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Long SLOT_ID = 6L;
    private static final Long STUDENT_ID = 3L;

    @Mock
    private LabReservationRepository reservationRepository;

    @Mock
    private EquipmentSlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    private ZonedDateTime start;
    private ZonedDateTime end;
    private EquipmentSlot slot;
    private User student;
    private AppUserPrincipal actor;
    private CreateReservationRequestDTO request;

    @BeforeEach
    void setUp() {
        start = ZonedDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        end = start.plusHours(2);

        User manager = new User("tec.fablab", "tecnico@utec.edu.pe", "hash", Role.ROLE_TECHNICIAN);
        Laboratory laboratory = new Laboratory("FabLab", "Pabellon A", manager, LaboratoryStatus.ACTIVE);

        slot = new EquipmentSlot(laboratory, "IMP-3D-04", start, end, 1);
        student = new User("raul.lab", "raul@utec.edu.pe", "hash", Role.ROLE_STUDENT);
        actor = new AppUserPrincipal(STUDENT_ID, "raul.lab", null, Role.ROLE_STUDENT);
        request = new CreateReservationRequestDTO("Prototipo del curso de Diseno");
    }

    @Test
    @DisplayName("Detecta solapamiento cuando el estudiante ya tiene una reserva en ese rango")
    void detectaSolapamientoDelEstudiante() {
        when(reservationRepository.countOverlappingForStudent(
                STUDENT_ID, start, end, ReservationStatus.RESERVED)).thenReturn(1L);

        assertThat(reservationService.hasOverlappingReservation(STUDENT_ID, start, end)).isTrue();
    }

    @Test
    @DisplayName("No hay solapamiento cuando el estudiante no tiene reservas en ese rango")
    void sinSolapamientoCuandoNoHayReservas() {
        when(reservationRepository.countOverlappingForStudent(
                STUDENT_ID, start, end, ReservationStatus.RESERVED)).thenReturn(0L);

        assertThat(reservationService.hasOverlappingReservation(STUDENT_ID, start, end)).isFalse();
    }

    @Test
    @DisplayName("Rechaza la reserva si el horario se solapa con otra reserva del estudiante")
    void rechazaReservaSolapada() {
        when(slotRepository.findByIdForUpdate(SLOT_ID)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsForSlotAndStudent(SLOT_ID, STUDENT_ID)).thenReturn(false);
        when(reservationRepository.countOverlappingForStudent(
                STUDENT_ID, start, end, ReservationStatus.RESERVED)).thenReturn(1L);

        assertThatThrownBy(() -> reservationService.reserve(SLOT_ID, request, actor))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("se solapa");

        verify(reservationRepository, never()).save(any(LabReservation.class));
    }

    @Test
    @DisplayName("Rechaza una segunda reserva del mismo estudiante para el mismo turno")
    void rechazaReservaDuplicada() {
        when(slotRepository.findByIdForUpdate(SLOT_ID)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsForSlotAndStudent(SLOT_ID, STUDENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> reservationService.reserve(SLOT_ID, request, actor))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ya tiene una reserva para este turno");

        verify(reservationRepository, never()).save(any(LabReservation.class));
    }

    @Test
    @DisplayName("Rechaza la reserva cuando el turno ya alcanzo su capacidad")
    void rechazaReservaSinCapacidad() {
        when(slotRepository.findByIdForUpdate(SLOT_ID)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsForSlotAndStudent(SLOT_ID, STUDENT_ID)).thenReturn(false);
        when(reservationRepository.countOverlappingForStudent(
                STUDENT_ID, start, end, ReservationStatus.RESERVED)).thenReturn(0L);
        when(reservationRepository.countBySlotAndStatus(SLOT_ID, ReservationStatus.RESERVED)).thenReturn(1L);

        assertThatThrownBy(() -> reservationService.reserve(SLOT_ID, request, actor))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("capacidad");
    }

    @Test
    @DisplayName("Rechaza la reserva si el turno no existe")
    void rechazaTurnoInexistente() {
        when(slotRepository.findByIdForUpdate(SLOT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.reserve(SLOT_ID, request, actor))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Crea la reserva y marca el turno como FULL al agotarse la capacidad")
    void creaReservaYMarcaTurnoLleno() {
        when(slotRepository.findByIdForUpdate(SLOT_ID)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsForSlotAndStudent(SLOT_ID, STUDENT_ID)).thenReturn(false);
        when(reservationRepository.countOverlappingForStudent(
                STUDENT_ID, start, end, ReservationStatus.RESERVED)).thenReturn(0L);
        when(reservationRepository.countBySlotAndStatus(SLOT_ID, ReservationStatus.RESERVED)).thenReturn(0L);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(reservationRepository.save(any(LabReservation.class))).thenAnswer(call -> call.getArgument(0));

        ReservationResponseDTO response = reservationService.reserve(SLOT_ID, request, actor);

        assertThat(response.studentUsername()).isEqualTo("raul.lab");
        assertThat(response.status()).isEqualTo("RESERVED");

        ArgumentCaptor<LabReservation> captor = ArgumentCaptor.forClass(LabReservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getPurpose()).isEqualTo("Prototipo del curso de Diseno");
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.RESERVED);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.FULL);
        verify(slotRepository).save(eq(slot));
    }
}
