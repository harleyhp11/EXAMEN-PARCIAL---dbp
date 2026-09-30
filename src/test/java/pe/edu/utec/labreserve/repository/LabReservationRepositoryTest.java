package pe.edu.utec.labreserve.repository;

import pe.edu.utec.labreserve.entity.EquipmentSlot;
import pe.edu.utec.labreserve.entity.LabReservation;
import pe.edu.utec.labreserve.entity.Laboratory;
import pe.edu.utec.labreserve.entity.LaboratoryStatus;
import pe.edu.utec.labreserve.entity.ReservationStatus;
import pe.edu.utec.labreserve.entity.Role;
import pe.edu.utec.labreserve.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class LabReservationRepositoryTest {

    @Autowired
    private LabReservationRepository reservationRepository;

    @Autowired
    private EquipmentSlotRepository slotRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User student;
    private EquipmentSlot slot;
    private ZonedDateTime base;

    @BeforeEach
    void setUp() {
        User manager = userRepository.save(
                new User("tec.fablab", "tecnico@utec.edu.pe", "hash", Role.ROLE_TECHNICIAN));
        student = userRepository.save(
                new User("raul.lab", "raul@utec.edu.pe", "hash", Role.ROLE_STUDENT));

        Laboratory fabLab = laboratoryRepository.save(
                new Laboratory("FabLab", "Pabellon A", manager, LaboratoryStatus.ACTIVE));

        base = ZonedDateTime.now().plusDays(5).withHour(9).withMinute(0).withSecond(0).withNano(0);
        slot = slotRepository.save(new EquipmentSlot(fabLab, "IMP-3D-04", base, base.plusHours(2), 2));
    }

    @Test
    @DisplayName("La restriccion unica impide que el mismo estudiante reserve dos veces el mismo turno")
    void restriccionUnicaDeReserva() {
        reservationRepository.saveAndFlush(
                new LabReservation(slot, student, "Primera reserva", ZonedDateTime.now()));

        LabReservation duplicada = new LabReservation(slot, student, "Segunda reserva", ZonedDateTime.now());

        assertThatThrownBy(() -> reservationRepository.saveAndFlush(duplicada))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("existsForSlotAndStudent reconoce la reserva ya registrada")
    void reconoceReservaExistente() {
        reservationRepository.saveAndFlush(
                new LabReservation(slot, student, "Prototipo", ZonedDateTime.now()));

        assertThat(reservationRepository.existsForSlotAndStudent(slot.getId(), student.getId())).isTrue();
        assertThat(reservationRepository.existsForSlotAndStudent(slot.getId(), 999L)).isFalse();
    }

    @Test
    @DisplayName("Cuenta las reservas del estudiante cuyo turno cruza el rango consultado")
    void cuentaReservasSolapadas() {
        reservationRepository.saveAndFlush(
                new LabReservation(slot, student, "Prototipo", ZonedDateTime.now()));

        long solapadas = reservationRepository.countOverlappingForStudent(
                student.getId(), base.plusHours(1), base.plusHours(3), ReservationStatus.RESERVED);
        long separadas = reservationRepository.countOverlappingForStudent(
                student.getId(), base.plusHours(5), base.plusHours(6), ReservationStatus.RESERVED);

        assertThat(solapadas).isEqualTo(1);
        assertThat(separadas).isZero();
    }

    @Test
    @DisplayName("Las reservas propias se devuelven ordenadas por reservedAt descendente")
    void listaReservasPropiasOrdenadas() {
        EquipmentSlot otro = slotRepository.save(
                new EquipmentSlot(slot.getLaboratory(), "CNC-01", base.plusDays(1), base.plusDays(1).plusHours(2), 1));

        reservationRepository.saveAndFlush(
                new LabReservation(slot, student, "Antigua", ZonedDateTime.now().minusDays(2)));
        reservationRepository.saveAndFlush(
                new LabReservation(otro, student, "Reciente", ZonedDateTime.now()));

        Page<LabReservation> result = reservationRepository.findMine(
                student.getId(), null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getPurpose()).isEqualTo("Reciente");
    }
}
