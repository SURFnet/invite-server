package guests.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column
    @NotNull
    private Authority authority;

    @Column(name = "eduperson_principal_name")
    @NotNull
    private String eduPersonPrincipalName;

    @Column(name = "given_name")
    private String givenName;

    @Column(name = "family_name")
    private String familyName;

    @Column
    private String email;

    @Column(name = "created_at")
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @OneToMany(mappedBy = "user", orphanRemoval = true, fetch = FetchType.EAGER,cascade = CascadeType.ALL)
    private Set<Aup> aups = new HashSet<>();

    @OneToMany(mappedBy = "user", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<UserRole> roles = new HashSet<>();

    public User(Institution institution, Authority authority, Map<String, Object> tokenAttributes) {
        this(authority,
                (String) tokenAttributes.get("eduperson_principal_name"),
                (String) tokenAttributes.get("given_name"),
                (String) tokenAttributes.get("family_name"),
                (String) tokenAttributes.get("email"),
                institution);
    }

    public User(Authority authority, String eppn, String givenName, String familyName, String email, Institution institution) {
        this.authority = authority;
        this.eduPersonPrincipalName = eppn;
        this.givenName = givenName;
        this.familyName = familyName;
        this.email = email;
        this.institution = institution;
    }
}
