package guests.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Entity(name = "invitation_roles")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class InvitationRole implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "end_date")
    private Instant endDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invitation_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private Invitation invitation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    public InvitationRole(Role role) {
        this.role = role;
    }

}
