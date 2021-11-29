package guests.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.Period;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "invitations")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Invitation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "intended_role")
    private Authority intendedRole;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column
    private String message;

    @Column
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String hash;

    @Column(name = "enforce_email_equality")
    private boolean enforceEmailEquality;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inviter_id")
    @JsonIgnore
    private User inviter;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @OneToMany(mappedBy = "invitation", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<InvitationRole> roles = new HashSet<>();

    public Invitation(Authority intendedRole, String message, boolean enforceEmailEquality, Set<InvitationRole> roles) {
        this.intendedRole = intendedRole;
        this.message = message;
        this.enforceEmailEquality = enforceEmailEquality;
        this.status = Status.OPEN;
        this.roles = roles;
        if (!CollectionUtils.isEmpty(roles)) {
            roles.forEach(role -> role.setInvitation(this));
        }
        this.defaults();
    }

    public Invitation(Authority intendedRole, Status status, String hash, User inviter) {
        this.intendedRole = intendedRole;
        this.status = status;
        this.hash = hash;
        this.inviter = inviter;
        this.institution = inviter.getInstitution();
        this.defaults();
    }

    public void defaults() {
        this.expiryDate = Instant.now().plus(Period.ofDays(14));
        this.createdAt = Instant.now();
    }

    @JsonIgnore
    public void addInvitationRole(InvitationRole role) {
        this.roles.add(role);
        role.setInvitation(this);
    }
}
