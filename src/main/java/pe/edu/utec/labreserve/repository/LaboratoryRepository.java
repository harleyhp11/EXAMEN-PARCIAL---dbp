package pe.edu.utec.labreserve.repository;

import pe.edu.utec.labreserve.entity.Laboratory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LaboratoryRepository extends JpaRepository<Laboratory, Long> {

    Optional<Laboratory> findByName(String name);

    boolean existsByName(String name);
}
