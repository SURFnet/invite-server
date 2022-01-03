package guests.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity(name = "users")
@NoArgsConstructor
@Getter
@Setter
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

    @Column(name = "unspecified_id")
    @NotNull
    private String unspecifiedId;

    @Column(name = "given_name")
    private String givenName;

    @Column(name = "family_name")
    private String familyName;

    @Column
    private String email;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "last_activity")
    private Instant lastActivity = Instant.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @OneToMany(mappedBy = "user", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Aup> aups = new HashSet<>();

    @OneToMany(mappedBy = "user", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<UserRole> roles = new HashSet<>();

    public User(Institution institution, Authority authority, Map<String, Object> tokenAttributes) {
        this(authority,
                (String) tokenAttributes.get("eduperson_principal_name"),
                (String) tokenAttributes.get("unspecified_id"),
                (String) tokenAttributes.get("given_name"),
                (String) tokenAttributes.get("family_name"),
                (String) tokenAttributes.get("email"),
                institution);
    }

    public User(Authority authority, String eppn, String unspecifiedId, String givenName, String familyName, String email, Institution institution) {
        this.authority = authority;
        this.eduPersonPrincipalName = eppn;
        this.unspecifiedId = unspecifiedId;
        this.givenName = givenName;
        this.familyName = familyName;
        this.email = email;
        this.institution = institution;
        this.createdAt = Instant.now();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return String.format("%s %s", givenName, familyName);
    }

    @JsonIgnore
    public void addUserRole(UserRole role) {
        this.roles.add(role);
        role.setUser(this);
    }

    @JsonIgnore
    public boolean hasChanged(Map<String, Object> tokenAttributes) {
        User user = new User(institution, authority, tokenAttributes);
        boolean changed = !this.toScimString().equals(user.toScimString());
        if (changed) {
            this.eduPersonPrincipalName = user.eduPersonPrincipalName;
            this.familyName = user.familyName;
            this.givenName = user.givenName;
            this.email = user.email;
        }
        return changed;
    }

    private String toScimString() {
        return String.format("%s%s%s%s",
                this.eduPersonPrincipalName,
                this.familyName,
                this.givenName,
                this.email);

    }

}
