package guests.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "institutions")
@NoArgsConstructor
@Getter
@Setter
@EntityListeners(NameHolderListener.class)
public class Institution implements Serializable, NameHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id")
    @NotNull
    private String entityId;

    @Column(name = "home_institution")
    @NotNull
    private String homeInstitution;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "aup_url")
    private String aupUrl;

    @Column(name = "aup_version")
    private String aupVersion;

    @Embedded
    private Auditable auditable = new Auditable();

    @OneToMany(mappedBy = "institution", orphanRemoval = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<InstitutionMembership> institutionMemberships = new HashSet<>();


    public Institution(String displayName, String entityId, String homeInstitution, String aupUrl, String aupVersion) {
        this.displayName = displayName;
        this.entityId = entityId;
        this.homeInstitution = homeInstitution;
        this.aupUrl = aupUrl;
        this.aupVersion = aupVersion;
    }

    public Institution(Institution institution) {
        this.entityId = institution.getEntityId();
        this.homeInstitution = institution.getHomeInstitution();
        this.displayName = institution.getDisplayName();
    }

    @Override
    @JsonIgnore
    public void nameUrnCompatibilityCheck() {
        this.homeInstitution = compatibleUrnName(this.homeInstitution);
    }


    @JsonIgnore
    public void invariantAupVersion(boolean isNew) {
        this.aupVersion = StringUtils.hasText(this.aupUrl) ? (isNew ? "1" : this.aupVersion) : null;
    }

    @JsonIgnore
    public void incrementAup() {
        if (StringUtils.hasText(this.aupUrl)) {
            int newVersion = Integer.parseInt(this.aupVersion) + 1;
            this.aupVersion = String.format("%s", newVersion);
        }
    }
}
