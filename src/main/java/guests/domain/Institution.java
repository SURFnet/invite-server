package guests.domain;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity(name = "institutions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Institution implements Serializable {

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

    public Institution(String entityId, String homeInstitution, String aupUrl, String aupVersion) {
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
}
