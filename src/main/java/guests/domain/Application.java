package guests.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import guests.exception.InvalidProvisioningException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.LazyInitializationException;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity(name = "applications")
@NoArgsConstructor
@Getter
@Setter
@EntityListeners(NameHolderListener.class)
public class Application implements Serializable, NameHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotNull
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "entity_id")
    @NotNull
    private String entityId;

    @Column(name = "landing_page")
    @NotNull
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

    @Column(name = "update_role_put_method")
    private boolean updateRolePutMethod;

    @Embedded
    private Auditable auditable = new Auditable();

    public Application(Institution institution) {
        this.institution = institution;
    }

    public Application(Institution institution,
                       String entityId,
                       String landingPage,
                       String provisioningHookUrl,
                       String provisioningHookUsername,
                       String provisioningHookPassword) {
        this.institution = institution;
        this.entityId = entityId;
        this.landingPage = landingPage;
        this.name = entityId;
        this.provisioningHookUrl = provisioningHookUrl;
        this.provisioningHookUsername = provisioningHookUsername;
        this.provisioningHookPassword = provisioningHookPassword;
    }

    @JsonProperty(value = "institution", access = JsonProperty.Access.READ_ONLY)
    public Map<String, Object> getInstitutionMap() {
        try {
            Institution institution = getInstitution();

            Map<String, Object> institutionMap = new HashMap<>();
            institutionMap.put("id", institution.getId());
            institutionMap.put("displayName", institution.getDisplayName());

            return institutionMap;
        } catch (LazyInitializationException e) {
            return null;
        }
    }

    @Override
    @JsonIgnore
    public void nameUrnCompatibilityCheck() {
        this.name = compatibleUrnName(this.name);
    }

    @JsonIgnore
    public void validateProvisioning() {
        if (StringUtils.hasText(provisioningHookUrl) &&
                (!StringUtils.hasText(provisioningHookUsername) || !StringUtils.hasText(provisioningHookPassword))) {
            throw new InvalidProvisioningException("provisioningHookUsername and provisioningHookPassword are required when provisioningHookUrl is configured");
        }
        if (StringUtils.hasText(provisioningHookUrl) && StringUtils.hasText(provisioningHookEmail)) {
            throw new InvalidProvisioningException("Can not specify both provisioningHookUrl and provisioningHookEmail");
        }
    }

    @JsonIgnore
    public void addRole(Role role) {
        this.roles.add(role);
        role.setApplication(this);
    }

    @JsonIgnore
    public boolean provisioningEnabled() {
        return StringUtils.hasText(provisioningHookUrl) || StringUtils.hasText(provisioningHookEmail);
    }

    @Override
    public String toString() {
        return "Application{" +
                "name='" + name + '\'' +
                ", entityId='" + entityId +
                '}';
    }

}
