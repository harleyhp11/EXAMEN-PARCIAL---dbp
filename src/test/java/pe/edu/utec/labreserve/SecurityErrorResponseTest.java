package pe.edu.utec.labreserve;

import pe.edu.utec.labreserve.entity.Laboratory;
import pe.edu.utec.labreserve.entity.LaboratoryStatus;
import pe.edu.utec.labreserve.entity.Role;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityErrorResponseTest {

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    @LocalServerPort
    private int port;

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

    private final HttpClient http = HttpClient.newHttpClient();

    private Long fabLabId;
    private Long otherLabId;
    private ZonedDateTime start;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        slotRepository.deleteAll();
        laboratoryRepository.deleteAll();
        userRepository.deleteAll();

        User technician = userRepository.save(new User(
                "tec.fablab", "tecnico@utec.edu.pe", passwordEncoder.encode("TecPass2026"), Role.ROLE_TECHNICIAN));
        User admin = userRepository.save(new User(
                "admin.utec", "admin@utec.edu.pe", passwordEncoder.encode("AdminPass2026"), Role.ROLE_ADMIN));
        userRepository.save(new User(
                "raul.lab", "raul@utec.edu.pe", passwordEncoder.encode("LabPass2026"), Role.ROLE_STUDENT));

        fabLabId = laboratoryRepository.save(new Laboratory(
                "FabLab", "Pabellon A", technician, LaboratoryStatus.ACTIVE)).getId();
        otherLabId = laboratoryRepository.save(new Laboratory(
                "Lab de Materiales", "Pabellon C", admin, LaboratoryStatus.ACTIVE)).getId();

        start = ZonedDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("Un estudiante publicando un turno recibe 403 con el ErrorResponseDTO, no 401")
    void rolInsuficienteDevuelve403() throws Exception {
        String token = login("raul.lab", "LabPass2026");
        String path = "/laboratories/" + fabLabId + "/slots";

        HttpResponse<String> response = post(path, slotBody(), token);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"status\":403");
        assertThat(response.body()).contains("\"error\":\"Forbidden\"");

        assertThat(response.body()).contains("\"path\":\"" + path + "\"");
    }

    @Test
    @DisplayName("Un tecnico publicando en un laboratorio que no gestiona recibe 403")
    void propiedadDelLaboratorioDevuelve403() throws Exception {
        String token = login("tec.fablab", "TecPass2026");
        String path = "/laboratories/" + otherLabId + "/slots";

        HttpResponse<String> response = post(path, slotBody(), token);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"status\":403");
        assertThat(response.body()).contains("\"path\":\"" + path + "\"");
    }

    @Test
    @DisplayName("Sin token la respuesta es 401 con el ErrorResponseDTO")
    void sinTokenDevuelve401() throws Exception {
        String path = "/laboratories/" + fabLabId + "/slots";

        HttpResponse<String> response = post(path, slotBody(), null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"status\":401");
        assertThat(response.body()).contains("\"path\":\"" + path + "\"");
    }

    @Test
    @DisplayName("Un token manipulado tambien devuelve 401")
    void tokenInvalidoDevuelve401() throws Exception {
        String path = "/laboratories/" + fabLabId + "/slots";

        HttpResponse<String> response = post(path, slotBody(), "no.es.un.token");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"status\":401");
    }

    @Test
    @DisplayName("Un laboratorio inexistente devuelve 404 aunque el rol sea suficiente")
    void laboratorioInexistenteDevuelve404() throws Exception {
        String token = login("admin.utec", "AdminPass2026");

        HttpResponse<String> response = post("/laboratories/999999/slots", slotBody(), token);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"status\":404");
    }

    private String login(String username, String password) throws Exception {
        HttpResponse<String> response = post("/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", null);
        assertThat(response.statusCode()).isEqualTo(200);

        Matcher matcher = TOKEN.matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private HttpResponse<String> post(String path, String body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String slotBody() {
        DateTimeFormatter iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        return """
                {"equipmentCode":"IMP-3D-04","startTime":"%s","endTime":"%s","capacity":1}
                """.formatted(iso.format(start), iso.format(start.plusHours(2)));
    }
}
