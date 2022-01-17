package guests.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.LazyInitializationException;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity(name = "aups")
@NoArgsConstructor
@Getter
@Setter
public class Aup implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Institution institution;

    @Column(name = "agreed_at")
    private Instant agreedAt;

    @Column
    private Integer version;

    @Column
    private String url;

    public Aup(Institution institution) {
        this.institution = institution;
        this.agreedAt = Instant.now();
        this.version = institution.getAupVersion();
        this.url = institution.getAupUrl();
    }

    @JsonProperty(value = "institution", access = JsonProperty.Access.READ_ONLY)
    public Map<String, Object> getInstitutionMap() {
        try {
            Institution institution = getInstitution();

            Map<String, Object> institutionMap = new HashMap<>();
            institutionMap.put("id", institution.getId());
            institutionMap.put("name", institution.getDisplayName());

            return institutionMap;
        } catch (LazyInitializationException e) {
            return null;
        }
    }

}
