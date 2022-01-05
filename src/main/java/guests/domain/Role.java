package guests.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Value;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

@Entity(name = "roles")
@NoArgsConstructor
@Getter
@Setter
@EntityListeners(NameHolderListener.class)
public class Role implements Serializable, NameHolder, ServiceProviderIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotNull
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "service_provider_id")
    private String serviceProviderId;

    @Enumerated(EnumType.STRING)
    @Column
    @NotNull
    private Authority authority = Authority.INVITER;

    @Column(name = "instant_available")
    private boolean instantAvailable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Application application;

    @Embedded
    private Auditable auditable = new Auditable();

    public Role(String name, Application application) {
        this.name = name;
        this.application = application;
    }

    @JsonProperty(value = "applicationName", access = JsonProperty.Access.READ_ONLY)
    public String getApplicationName() {
        try {
            return this.getApplication().getName();
        } catch (LazyInitializationException e) {
            return null;
        }
    }

    @JsonProperty(value = "institutionId", access = JsonProperty.Access.READ_ONLY)
    public Long getInstitutionId() {
        try {
            return this.getApplication().getInstitution().getId();
        } catch (LazyInitializationException e) {
            return null;
        }
    }

    @Override
    @JsonIgnore
    public void nameUrnCompatibilityCheck() {
        this.name = compatibleUrnName(this.name);
    }

}
