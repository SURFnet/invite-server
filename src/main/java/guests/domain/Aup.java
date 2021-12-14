package guests.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

@Entity(name = "aups")
@NoArgsConstructor
@AllArgsConstructor
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
    private String version;

    @Column
    private String url;

    public Aup(User user, Institution institution) {
        this.user = user;
        this.institution = institution;
        this.agreedAt = Instant.now();
        this.version = institution.getAupVersion();
        this.url = institution.getAupUrl();
    }
}
