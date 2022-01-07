package guests.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.LazyInitializationException;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

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

    @JsonProperty(value = "institutionId", access = JsonProperty.Access.READ_ONLY)
    public Long getInstitutionId() {
        try {
            return this.getInstitution().getId();
        } catch (LazyInitializationException | NullPointerException e) {
            return null;
        }
    }

}
