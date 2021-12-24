package guests.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

@Entity(name = "user_roles")
@NoArgsConstructor
@Getter
@Setter
public class UserRole implements Serializable, ServiceProviderIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_provider_id")
    private String serviceProviderId;

    @Column(name = "end_date")
    private Instant endDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    public UserRole(Role role, Instant endDate) {
        this.role = role;
        this.endDate = endDate;
    }
}
