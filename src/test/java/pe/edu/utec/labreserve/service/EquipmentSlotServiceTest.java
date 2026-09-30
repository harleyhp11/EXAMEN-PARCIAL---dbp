package pe.edu.utec.labreserve.service;

import pe.edu.utec.labreserve.dto.CreateSlotRequestDTO;
import pe.edu.utec.labreserve.dto.SlotResponseDTO;
import pe.edu.utec.labreserve.entity.EquipmentSlot;
import pe.edu.utec.labreserve.entity.Laboratory;
import pe.edu.utec.labreserve.entity.LaboratoryStatus;
import pe.edu.utec.labreserve.entity.Role;
import pe.edu.utec.labreserve.entity.SlotStatus;
import pe.edu.utec.labreserve.entity.User;
import pe.edu.utec.labreserve.exception.BusinessRuleException;
import pe.edu.utec.labreserve.exception.InvalidRequestException;
import pe.edu.utec.labreserve.repository.EquipmentSlotRepository;
import pe.edu.utec.labreserve.repository.LaboratoryRepository;
import pe.edu.utec.labreserve.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentSlotServiceTest {

    private static final Long LAB_ID = 1L;
    private static final Long TECHNICIAN_ID = 2L;

    @Mock
    private EquipmentSlotRepository slotRepository;

    @Mock
    private LaboratoryRepository laboratoryRepository;

    @InjectMocks
    private EquipmentSlotService slotService;

    private Laboratory fabLab;
    private AppUserPrincipal technician;
    private ZonedDateTime start;
    private ZonedDateTime end;

    @BeforeEach
    void setUp() {
        User manager = new User("tec.fablab", "tecnico@utec.edu.pe", "hash", Role.ROLE_TECHNICIAN);
        fabLab = new Laboratory("FabLab", "Pabellon A", manager, LaboratoryStatus.ACTIVE);
        technician = new AppUserPrincipal(TECHNICIAN_ID, "tec.fablab", null, Role.ROLE_TECHNICIAN);

        start = ZonedDateTime.now().plusDays(4).withHour(10).withMinute(0).withSecond(0).withNano(0);
        end = start.plusHours(2);
    }

    @Test
    @DisplayName("Un turno de 10 a 12 se solapa con otro de 11 a 13 del mismo equipo")
    void detectaSolapamientoParcial() {
        EquipmentSlot existente = new EquipmentSlot(fabLab, "IMP-3D-04", start, end, 1);
        when(slotRepository.findActiveByLaboratoryAndEquipment(LAB_ID, "IMP-3D-04", SlotStatus.CANCELLED))
                .thenReturn(List.of(existente));

        boolean overlaps = slotService.hasOverlap(LAB_ID, "IMP-3D-04", start.plusHours(1), end.plusHours(1));

        assertThat(overlaps).isTrue();
    }

    @Test
    @DisplayName("Dos turnos contiguos no se consideran solapados")
    void turnosContiguosNoSeSolapan() {
        EquipmentSlot existente = new EquipmentSlot(fabLab, "IMP-3D-04", start, end, 1);
        when(slotRepository.findActiveByLaboratoryAndEquipment(LAB_ID, "IMP-3D-04", SlotStatus.CANCELLED))
                .thenReturn(List.of(existente));

        boolean overlaps = slotService.hasOverlap(LAB_ID, "IMP-3D-04", end, end.plusHours(2));

        assertThat(overlaps).isFalse();
    }

    @Test
    @DisplayName("Rechaza publicar un turno que se solapa con otro del mismo equipo")
    void rechazaTurnoSolapado() {
        EquipmentSlot existente = new EquipmentSlot(fabLab, "IMP-3D-04", start, end, 1);
        when(laboratoryRepository.findById(LAB_ID)).thenReturn(Optional.of(fabLab));
        when(slotRepository.findActiveByLaboratoryAndEquipment(LAB_ID, "IMP-3D-04", SlotStatus.CANCELLED))
                .thenReturn(List.of(existente));

        CreateSlotRequestDTO request = new CreateSlotRequestDTO(
                "IMP-3D-04", start.plusHours(1), end.plusHours(1), 1);

        assertThatThrownBy(() -> slotService.createSlot(LAB_ID, request, adminActor()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("solapa");

        verify(slotRepository, never()).save(any(EquipmentSlot.class));
    }

    @Test
    @DisplayName("Rechaza publicar un turno en un laboratorio en MAINTENANCE")
    void rechazaLaboratorioEnMantenimiento() {
        fabLab.setStatus(LaboratoryStatus.MAINTENANCE);
        when(laboratoryRepository.findById(LAB_ID)).thenReturn(Optional.of(fabLab));

        CreateSlotRequestDTO request = new CreateSlotRequestDTO("IMP-3D-04", start, end, 1);

        assertThatThrownBy(() -> slotService.createSlot(LAB_ID, request, adminActor()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    @DisplayName("Rechaza un turno cuyo startTime no es anterior al endTime")
    void rechazaRangoInvertido() {
        when(laboratoryRepository.findById(LAB_ID)).thenReturn(Optional.of(fabLab));

        CreateSlotRequestDTO request = new CreateSlotRequestDTO("IMP-3D-04", end, start, 1);

        assertThatThrownBy(() -> slotService.createSlot(LAB_ID, request, adminActor()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("anterior");
    }

    @Test
    @DisplayName("Un tecnico no puede publicar turnos en un laboratorio que no gestiona")
    void rechazaTecnicoQueNoGestionaElLaboratorio() {
        when(laboratoryRepository.findById(LAB_ID)).thenReturn(Optional.of(fabLab));

        CreateSlotRequestDTO request = new CreateSlotRequestDTO("IMP-3D-04", start, end, 1);

        assertThatThrownBy(() -> slotService.createSlot(LAB_ID, request, technician))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Publica el turno con estado inicial AVAILABLE")
    void publicaTurnoDisponible() {
        when(laboratoryRepository.findById(LAB_ID)).thenReturn(Optional.of(fabLab));
        when(slotRepository.findActiveByLaboratoryAndEquipment(anyLong(), anyString(), any()))
                .thenReturn(List.of());
        when(slotRepository.save(any(EquipmentSlot.class))).thenAnswer(call -> call.getArgument(0));

        CreateSlotRequestDTO request = new CreateSlotRequestDTO("IMP-3D-04", start, end, 1);
        SlotResponseDTO response = slotService.createSlot(LAB_ID, request, adminActor());

        assertThat(response.status()).isEqualTo("AVAILABLE");
        assertThat(response.laboratoryName()).isEqualTo("FabLab");
        assertThat(response.equipmentCode()).isEqualTo("IMP-3D-04");
    }

    private AppUserPrincipal adminActor() {
        return new AppUserPrincipal(99L, "admin.utec", null, Role.ROLE_ADMIN);
    }
}
