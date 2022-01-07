package guests.api;

import guests.domain.Authority;
import guests.domain.Institution;
import guests.domain.InstitutionMembership;
import guests.domain.User;
import guests.exception.UserRestrictionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SharedTest {

    @Test
    void verifyAuthority() {
        Shared shared = new Shared();
        User user = new User();
        Institution institution = new Institution();
        institution.setId(1L);
        user.addMembership(new InstitutionMembership(Authority.GUEST, institution));
        assertThrows(UserRestrictionException.class, () -> shared.verifyAuthority(user, institution.getId(), Authority.INVITER));

        shared.verifyAuthority(user, institution.getId(), Authority.GUEST);
    }
}