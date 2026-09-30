package pe.edu.utec.labreserve.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "equipment_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_slot_lab_equipment_start",
                columnNames = {"laboratory_id", "equipment_code", "start_time"}
        )
)
public class EquipmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laboratory_id", nullable = false)
    private Laboratory laboratory;

    @Column(name = "equipment_code", nullable = false, length = 50)
    private String equipmentCode;

    @Column(name = "start_time", nullable = false)
    private ZonedDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private ZonedDateTime endTime;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SlotStatus status;

    protected EquipmentSlot() {

    }

    public EquipmentSlot(Laboratory laboratory,
                         String equipmentCode,
                         ZonedDateTime startTime,
                         ZonedDateTime endTime,
                         Integer capacity) {
        this.laboratory = laboratory;
        this.equipmentCode = equipmentCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.status = SlotStatus.AVAILABLE;
    }

    public boolean overlapsWith(ZonedDateTime otherStart, ZonedDateTime otherEnd) {
        return startTime.isBefore(otherEnd) && endTime.isAfter(otherStart);
    }

    public Long getId() {
        return id;
    }

    public Laboratory getLaboratory() {
        return laboratory;
    }

    public void setLaboratory(Laboratory laboratory) {
        this.laboratory = laboratory;
    }

    public Long getLaboratoryId() {
        return laboratory == null ? null : laboratory.getId();
    }

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public void setEquipmentCode(String equipmentCode) {
        this.equipmentCode = equipmentCode;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }
}
