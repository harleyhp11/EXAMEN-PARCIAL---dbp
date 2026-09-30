package pe.edu.utec.labreserve.config;

import pe.edu.utec.labreserve.entity.EquipmentSlot;
import pe.edu.utec.labreserve.entity.Laboratory;
import pe.edu.utec.labreserve.entity.LaboratoryStatus;
import pe.edu.utec.labreserve.entity.Role;
import pe.edu.utec.labreserve.entity.User;
import pe.edu.utec.labreserve.repository.EquipmentSlotRepository;
import pe.edu.utec.labreserve.repository.LaboratoryRepository;
import pe.edu.utec.labreserve.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final EquipmentSlotRepository slotRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      LaboratoryRepository laboratoryRepository,
                      EquipmentSlotRepository slotRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.slotRepository = slotRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(new User(
                "admin.utec", "admin@utec.edu.pe", passwordEncoder.encode("AdminPass2026"), Role.ROLE_ADMIN));
        User technician = userRepository.save(new User(
                "tec.fablab", "tecnico@utec.edu.pe", passwordEncoder.encode("TecPass2026"), Role.ROLE_TECHNICIAN));
        userRepository.save(new User(
                "raul.lab", "raul@utec.edu.pe", passwordEncoder.encode("LabPass2026"), Role.ROLE_STUDENT));

        Laboratory fabLab = laboratoryRepository.save(new Laboratory(
                "FabLab", "Pabellon A - Piso 2", technician, LaboratoryStatus.ACTIVE));
        laboratoryRepository.save(new Laboratory(
                "Lab de Robotica", "Pabellon B - Piso 1", admin, LaboratoryStatus.MAINTENANCE));

        ZonedDateTime base = ZonedDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);
        slotRepository.save(new EquipmentSlot(fabLab, "IMP-3D-04", base, base.plusHours(2), 1));
        slotRepository.save(new EquipmentSlot(fabLab, "CNC-01", base.plusDays(1), base.plusDays(1).plusHours(3), 2));
    }
}
