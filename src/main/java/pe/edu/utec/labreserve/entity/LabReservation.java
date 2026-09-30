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
        name = "lab_reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_slot_student",
                columnNames = {"slot_id", "student_id"}
        )
)
public class LabReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private EquipmentSlot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 250)
    private String purpose;

    @Column(name = "reserved_at", nullable = false)
    private ZonedDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    protected LabReservation() {

    }

    public LabReservation(EquipmentSlot slot, User student, String purpose, ZonedDateTime reservedAt) {
        this.slot = slot;
        this.student = student;
        this.purpose = purpose;
        this.reservedAt = reservedAt;
        this.status = ReservationStatus.RESERVED;
    }

    public Long getId() {
        return id;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public void setSlot(EquipmentSlot slot) {
        this.slot = slot;
    }

    public Long getSlotId() {
        return slot == null ? null : slot.getId();
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Long getStudentId() {
        return student == null ? null : student.getId();
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public ZonedDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(ZonedDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
