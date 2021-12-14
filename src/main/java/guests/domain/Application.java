package guests.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "applications")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Application implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "entity_id")
    @NotNull
    private String entityId;

    @Column(name = "landing_page")
    private String landingPage;

    @Column(name = "provisioning_hook_url")
    private String provisioningHookUrl;

    @Column(name = "provisioning_hook_username")
    private String provisioningHookUsername;

    @Column(name = "provisioning_hook_password")
    private String provisioningHookPassword;

    @Column(name = "provisioning_hook_email")
    private String provisioningHookEmail;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Institution institution;

    @OneToMany(mappedBy = "application", orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<Role> roles = new HashSet<>();

    @Embedded
    private Auditable auditable = new Auditable();

    public Application(Institution institution, String entityId) {
        this.institution = institution;
        this.entityId = entityId;
        this.displayName = entityId;
    }

}
