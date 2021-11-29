package guests.domain;

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
@ToString
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

    @Column(name = "provisioning_hook_email")
    private String provisioningHookEmail;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id")
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
