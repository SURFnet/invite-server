package guests.api;

import guests.AbstractTest;
import guests.domain.Authority;
import guests.domain.User;
import guests.exception.UserRestrictionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SharedTest {

    @Test
    void verifyAuthority() {
        Shared shared = new Shared();
        User user = new User();
        user.setAuthority(Authority.GUEST);
        assertThrows(UserRestrictionException.class, () -> shared.verifyAuthority(user, Authority.INVITER));

        user.setAuthority(Authority.INVITER);
        assertThrows(UserRestrictionException.class, () -> shared.verifyAuthority(user, Authority.INVITER));

        shared.verifyAuthority(user, Authority.GUEST);
    }
}