package pe.edu.utec.labreserve;

import pe.edu.utec.labreserve.entity.EquipmentSlot;
import pe.edu.utec.labreserve.entity.Laboratory;
import pe.edu.utec.labreserve.entity.LaboratoryStatus;
import pe.edu.utec.labreserve.entity.Role;
import pe.edu.utec.labreserve.entity.SlotStatus;
import pe.edu.utec.labreserve.entity.User;
import pe.edu.utec.labreserve.repository.EquipmentSlotRepository;
import pe.edu.utec.labreserve.repository.LabReservationRepository;
import pe.edu.utec.labreserve.repository.LaboratoryRepository;
import pe.edu.utec.labreserve.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LabReserveFlowTest {

    private static final String PURPOSE = """
            {"purpose":"Prototipo del curso de Diseno"}
            """;

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private EquipmentSlotRepository slotRepository;

    @Autowired
    private LabReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long fabLabId;
    private ZonedDateTime start;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        slotRepository.deleteAll();
        laboratoryRepository.deleteAll();
        userRepository.deleteAll();

        User technician = userRepository.save(new User(
                "tec.fablab", "tecnico@utec.edu.pe", passwordEncoder.encode("TecPass2026"), Role.ROLE_TECHNICIAN));
        userRepository.save(new User(
                "raul.lab", "raul@utec.edu.pe", passwordEncoder.encode("LabPass2026"), Role.ROLE_STUDENT));
        userRepository.save(new User(
                "ana.lab", "ana@utec.edu.pe", passwordEncoder.encode("LabPass2026"), Role.ROLE_STUDENT));

        fabLabId = laboratoryRepository.save(new Laboratory(
                "FabLab", "Pabellon A", technician, LaboratoryStatus.ACTIVE)).getId();

        start = ZonedDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("Registro, login, publicacion de turno, busqueda, reserva y consulta propia")
    void flujoCompleto() throws Exception {

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nuevo.alumno","email":"nuevo@utec.edu.pe","password":"LabPass2026"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("nuevo.alumno"))
                .andExpect(jsonPath("$.email").value("nuevo@utec.edu.pe"))
                .andExpect(jsonPath("$.password").doesNotExist());

        String technicianToken = login("tec.fablab", "TecPass2026");

        String slotResponse = mockMvc.perform(post("/laboratories/" + fabLabId + "/slots")
                        .header("Authorization", "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody("IMP-3D-04", start, start.plusHours(2), 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.laboratoryName").value("FabLab"))
                .andExpect(jsonPath("$.equipmentCode").value("IMP-3D-04"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn().getResponse().getContentAsString();

        long slotId = Long.parseLong(slotResponse.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));

        mockMvc.perform(get("/equipment-slots").param("equipmentCode", "IMP-3D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content[0].equipmentCode").value("IMP-3D-04"));

        String studentToken = login("raul.lab", "LabPass2026");

        mockMvc.perform(post("/equipment-slots/" + slotId + "/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purpose":"Prototipo del curso de Diseno"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotId").value(slotId))
                .andExpect(jsonPath("$.studentUsername").value("raul.lab"))
                .andExpect(jsonPath("$.status").value("RESERVED"));

        mockMvc.perform(get("/my-lab-reservations")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].equipmentCode").value("IMP-3D-04"))
                .andExpect(jsonPath("$.content[0].status").value("RESERVED"));

        assertThat(slotRepository.findById(slotId).orElseThrow().getStatus().name()).isEqualTo("FULL");
        mockMvc.perform(get("/equipment-slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Sin token la publicacion de turnos responde 401 con el error comun")
    void sinTokenNoSePuedePublicar() throws Exception {
        mockMvc.perform(post("/laboratories/" + fabLabId + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody("IMP-3D-04", start, start.plusHours(2), 1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/laboratories/" + fabLabId + "/slots"));
    }

    @Test
    @DisplayName("Un estudiante no puede publicar turnos")
    void estudianteNoPuedePublicar() throws Exception {
        String studentToken = login("raul.lab", "LabPass2026");

        mockMvc.perform(post("/laboratories/" + fabLabId + "/slots")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody("IMP-3D-04", start, start.plusHours(2), 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Credenciales invalidas devuelven 401")
    void loginConCredencialesInvalidas() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"raul.lab","password":"ClaveIncorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Un password de menos de 8 caracteres devuelve 400 con el detalle del campo")
    void registroConPasswordCorto() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"corto","email":"corto@utec.edu.pe","password":"1234"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value("password: password debe tener al menos 8 caracteres"));
    }

    @Test
    @DisplayName("Un username repetido devuelve 409")
    void registroConUsernameRepetido() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"raul.lab","email":"otro@utec.edu.pe","password":"LabPass2026"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Rechaza un turno que se solapa con otro del mismo equipo")
    void rechazaTurnoSolapado() throws Exception {
        String technicianToken = login("tec.fablab", "TecPass2026");

        mockMvc.perform(post("/laboratories/" + fabLabId + "/slots")
                        .header("Authorization", "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody("IMP-3D-04", start, start.plusHours(2), 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/laboratories/" + fabLabId + "/slots")
                        .header("Authorization", "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody("IMP-3D-04", start.plusHours(1), start.plusHours(3), 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("El mismo estudiante no puede reservar dos veces el mismo turno")
    void rechazaReservaDuplicada() throws Exception {
        String technicianToken = login("tec.fablab", "TecPass2026");
        EquipmentSlot slot = slotRepository.save(new EquipmentSlot(
                laboratoryRepository.findById(fabLabId).orElseThrow(),
                "CNC-01", start, start.plusHours(2), 5));
        assertThat(technicianToken).isNotBlank();

        String studentToken = login("raul.lab", "LabPass2026");
        String body = """
                {"purpose":"Primera reserva"}
                """;

        mockMvc.perform(post("/equipment-slots/" + slot.getId() + "/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/equipment-slots/" + slot.getId() + "/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Publicar un turno en un laboratorio inexistente devuelve 404")
    void turnoEnLaboratorioInexistente() throws Exception {
        String technicianToken = login("tec.fablab", "TecPass2026");

        mockMvc.perform(post("/laboratories/999999/slots")
                        .header("Authorization", "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody("IMP-3D-04", start, start.plusHours(2), 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Laboratorio con id 999999 no existe"));
    }

    @Test
    @DisplayName("Reservar un turno inexistente devuelve 404")
    void reservaDeTurnoInexistente() throws Exception {
        String studentToken = login("raul.lab", "LabPass2026");

        mockMvc.perform(post("/equipment-slots/999999/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PURPOSE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Turno con id 999999 no existe"));
    }

    @Test
    @DisplayName("Un turno sin capacidad devuelve 409 al siguiente estudiante")
    void turnoSinCapacidad() throws Exception {
        EquipmentSlot slot = slotRepository.save(new EquipmentSlot(
                laboratoryRepository.findById(fabLabId).orElseThrow(),
                "IMP-3D-04", start, start.plusHours(2), 1));

        mockMvc.perform(post("/equipment-slots/" + slot.getId() + "/reservations")
                        .header("Authorization", "Bearer " + login("raul.lab", "LabPass2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PURPOSE))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/equipment-slots/" + slot.getId() + "/reservations")
                        .header("Authorization", "Bearer " + login("ana.lab", "LabPass2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PURPOSE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("El turno ya no tiene capacidad disponible"));
    }

    @Test
    @DisplayName("Un turno cancelado no admite reservas")
    void turnoCancelado() throws Exception {
        EquipmentSlot slot = slotRepository.save(new EquipmentSlot(
                laboratoryRepository.findById(fabLabId).orElseThrow(),
                "CNC-01", start, start.plusHours(2), 3));
        slot.setStatus(SlotStatus.CANCELLED);
        slotRepository.saveAndFlush(slot);

        mockMvc.perform(post("/equipment-slots/" + slot.getId() + "/reservations")
                        .header("Authorization", "Bearer " + login("raul.lab", "LabPass2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PURPOSE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("El turno esta cancelado"));
    }

    @Test
    @DisplayName("Reservar dos turnos que se cruzan en el tiempo devuelve 409")
    void reservaSuperpuesta() throws Exception {
        Laboratory fabLab = laboratoryRepository.findById(fabLabId).orElseThrow();
        EquipmentSlot primero = slotRepository.save(new EquipmentSlot(
                fabLab, "IMP-3D-04", start, start.plusHours(2), 5));

        EquipmentSlot cruzado = slotRepository.save(new EquipmentSlot(
                fabLab, "CNC-01", start.plusHours(1), start.plusHours(3), 5));

        String studentToken = login("raul.lab", "LabPass2026");

        mockMvc.perform(post("/equipment-slots/" + primero.getId() + "/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PURPOSE))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/equipment-slots/" + cruzado.getId() + "/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PURPOSE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya tiene una reserva que se solapa con ese horario"));
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn().getResponse().getContentAsString();

        Matcher matcher = TOKEN.matcher(response);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String slotBody(String equipmentCode, ZonedDateTime from, ZonedDateTime to, int capacity) {
        DateTimeFormatter iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        return """
                {"equipmentCode":"%s","startTime":"%s","endTime":"%s","capacity":%d}
                """.formatted(equipmentCode, iso.format(from), iso.format(to), capacity);
    }
}
