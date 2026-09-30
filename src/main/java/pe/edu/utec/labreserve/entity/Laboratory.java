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

@Entity
@Table(
        name = "laboratories",
        uniqueConstraints = @UniqueConstraint(name = "uk_laboratories_name", columnNames = "name")
)
public class Laboratory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LaboratoryStatus status;

    protected Laboratory() {

    }

    public Laboratory(String name, String location, User manager, LaboratoryStatus status) {
        this.name = name;
        this.location = location;
        this.manager = manager;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public Long getManagerId() {
        return manager == null ? null : manager.getId();
    }

    public LaboratoryStatus getStatus() {
        return status;
    }

    public void setStatus(LaboratoryStatus status) {
        this.status = status;
    }
}
