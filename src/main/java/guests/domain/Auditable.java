package guests.domain;

import lombok.Getter;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Embeddable
@Getter
public class Auditable implements Serializable {

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        createdBy = SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        updatedBy = SecurityContextHolder.getContext().getAuthentication().getName();
    }

}
