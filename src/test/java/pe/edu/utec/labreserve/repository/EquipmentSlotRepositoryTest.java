package pe.edu.utec.labreserve.repository;

import pe.edu.utec.labreserve.entity.EquipmentSlot;
import pe.edu.utec.labreserve.entity.Laboratory;
import pe.edu.utec.labreserve.entity.LaboratoryStatus;
import pe.edu.utec.labreserve.entity.Role;
import pe.edu.utec.labreserve.entity.SlotStatus;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class EquipmentSlotRepositoryTest {

    @Autowired
    private EquipmentSlotRepository slotRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Laboratory fabLab;
    private Laboratory roboticsLab;
    private ZonedDateTime base;

    @BeforeEach
    void setUp() {
        User manager = userRepository.save(
                new User("tec.fablab", "tecnico@utec.edu.pe", "hash", Role.ROLE_TECHNICIAN));

        fabLab = laboratoryRepository.save(
                new Laboratory("FabLab", "Pabellon A", manager, LaboratoryStatus.ACTIVE));
        roboticsLab = laboratoryRepository.save(
                new Laboratory("Lab de Robotica", "Pabellon B", manager, LaboratoryStatus.ACTIVE));

        base = ZonedDateTime.now().plusDays(5).withHour(9).withMinute(0).withSecond(0).withNano(0);

        slotRepository.save(new EquipmentSlot(fabLab, "IMP-3D-04", base, base.plusHours(2), 1));
        slotRepository.save(new EquipmentSlot(fabLab, "IMP-3D-05", base.plusDays(1), base.plusDays(1).plusHours(2), 2));
        slotRepository.save(new EquipmentSlot(fabLab, "CNC-01", base.plusDays(2), base.plusDays(2).plusHours(2), 3));
        slotRepository.save(new EquipmentSlot(roboticsLab, "IMP-3D-09", base.plusDays(3), base.plusDays(3).plusHours(2), 1));
    }

    @Test
    @DisplayName("Busca turnos por coincidencia parcial del codigo de equipo")
    void buscaPorCodigoDeEquipoParcial() {
        Page<EquipmentSlot> result = slotRepository.search(
                null, "IMP-3D", null, ZonedDateTime.now(), SlotStatus.AVAILABLE, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(EquipmentSlot::getEquipmentCode)
                .containsExactlyInAnyOrder("IMP-3D-04", "IMP-3D-05", "IMP-3D-09");
    }

    @Test
    @DisplayName("Combina el filtro de laboratorio con el de codigo de equipo")
    void combinaFiltrosDeLaboratorioYEquipo() {
        Page<EquipmentSlot> result = slotRepository.search(
                fabLab.getId(), "IMP-3D", null, ZonedDateTime.now(), SlotStatus.AVAILABLE, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .allMatch(slot -> slot.getLaboratoryId().equals(fabLab.getId()));
    }

    @Test
    @DisplayName("Sin filtros devuelve todos los turnos AVAILABLE futuros, paginados")
    void devuelveTodosLosTurnosFuturosPaginados() {
        Page<EquipmentSlot> firstPage = slotRepository.search(
                null, null, null, ZonedDateTime.now(), SlotStatus.AVAILABLE, PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getNumber()).isZero();
    }

    @Test
    @DisplayName("El filtro from descarta los turnos anteriores a la fecha indicada")
    void filtraDesdeUnaFecha() {
        Page<EquipmentSlot> result = slotRepository.search(
                null, null, base.plusDays(2), ZonedDateTime.now(), SlotStatus.AVAILABLE, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Excluye los turnos que no estan AVAILABLE")
    void excluyeTurnosNoDisponibles() {
        EquipmentSlot cancelled = slotRepository.save(
                new EquipmentSlot(fabLab, "LASER-02", base.plusDays(4), base.plusDays(4).plusHours(1), 1));
        cancelled.setStatus(SlotStatus.CANCELLED);
        slotRepository.saveAndFlush(cancelled);

        Page<EquipmentSlot> result = slotRepository.search(
                null, "LASER", null, ZonedDateTime.now(), SlotStatus.AVAILABLE, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Trae los turnos activos del mismo equipo para evaluar solapamiento")
    void listaTurnosActivosDelEquipo() {
        List<EquipmentSlot> active = slotRepository.findActiveByLaboratoryAndEquipment(
                fabLab.getId(), "IMP-3D-04", SlotStatus.CANCELLED);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getEquipmentCode()).isEqualTo("IMP-3D-04");
    }

    @Test
    @DisplayName("La restriccion unica impide dos turnos del mismo equipo con el mismo inicio")
    void restriccionUnicaDeTurno() {
        EquipmentSlot duplicado = new EquipmentSlot(fabLab, "IMP-3D-04", base, base.plusHours(4), 1);

        assertThatThrownBy(() -> slotRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
