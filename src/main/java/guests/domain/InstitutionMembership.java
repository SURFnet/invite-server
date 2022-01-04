package guests.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity(name = "institution_memberships")
@NoArgsConstructor
@Getter
@Setter
public class InstitutionMembership implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column
    @NotNull
    private Authority authority;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    public InstitutionMembership(Authority authority, Institution institution) {
        this.authority = authority;
        this.institution = institution;
    }

    public InstitutionMembership(Authority authority, Institution institution, User user) {
        this.authority = authority;
        this.institution = institution;
        this.user = user;
    }
}
