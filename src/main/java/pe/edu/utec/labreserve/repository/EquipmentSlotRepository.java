package pe.edu.utec.labreserve.repository;

import jakarta.persistence.LockModeType;
import pe.edu.utec.labreserve.entity.EquipmentSlot;
import pe.edu.utec.labreserve.entity.SlotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface EquipmentSlotRepository extends JpaRepository<EquipmentSlot, Long> {

    @Query("""
            SELECT s FROM EquipmentSlot s
            WHERE s.laboratory.id = :laboratoryId
              AND s.equipmentCode = :equipmentCode
              AND s.status <> :cancelled
            """)
    List<EquipmentSlot> findActiveByLaboratoryAndEquipment(@Param("laboratoryId") Long laboratoryId,
                                                           @Param("equipmentCode") String equipmentCode,
                                                           @Param("cancelled") SlotStatus cancelled);

    @Query(value = """
            SELECT s FROM EquipmentSlot s
            WHERE s.status = :status
              AND s.startTime > :now
              AND (:laboratoryId IS NULL OR s.laboratory.id = :laboratoryId)
              AND (:equipmentCode IS NULL OR LOWER(s.equipmentCode) LIKE LOWER(CONCAT('%', :equipmentCode, '%')))
              AND (:from IS NULL OR s.startTime >= :from)
            ORDER BY s.startTime ASC
            """,
            countQuery = """
            SELECT COUNT(s) FROM EquipmentSlot s
            WHERE s.status = :status
              AND s.startTime > :now
              AND (:laboratoryId IS NULL OR s.laboratory.id = :laboratoryId)
              AND (:equipmentCode IS NULL OR LOWER(s.equipmentCode) LIKE LOWER(CONCAT('%', :equipmentCode, '%')))
              AND (:from IS NULL OR s.startTime >= :from)
            """)
    Page<EquipmentSlot> search(@Param("laboratoryId") Long laboratoryId,
                               @Param("equipmentCode") String equipmentCode,
                               @Param("from") ZonedDateTime from,
                               @Param("now") ZonedDateTime now,
                               @Param("status") SlotStatus status,
                               Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM EquipmentSlot s WHERE s.id = :id")
    Optional<EquipmentSlot> findByIdForUpdate(@Param("id") Long id);
}
