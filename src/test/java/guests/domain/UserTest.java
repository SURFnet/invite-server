package guests.domain;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void hasChanged() {
        Map<String, Object> tokenAttributes = new HashMap<>();
        tokenAttributes.put("eduperson_principal_name", "eppn@example.com");
        tokenAttributes.put("unspecified_id", "urn:collab:test");
        tokenAttributes.put("given_name", "John");
        tokenAttributes.put("family_name", "Doe");
        tokenAttributes.put("email", "jdoe@example.com");

        User user = new User(new Institution(), Authority.SUPER_ADMIN, tokenAttributes);
        assertFalse(user.hasChanged(tokenAttributes));

        tokenAttributes.put("family_name", "Does");
        assertTrue(user.hasChanged(tokenAttributes));
    }

    @Test
    void hasAgreedWithAup() {
        User user = new User();
        Institution institution = new Institution();
        institution.setId(1L);
        assertTrue(user.hasAgreedWithAup(institution));

        institution.setAupUrl("https://aup");
        institution.setAupVersion(1);
        assertFalse(user.hasAgreedWithAup(institution));

        user.addAup(new Aup(institution));
        assertTrue(user.hasAgreedWithAup(institution));

        institution.incrementAup();
        assertFalse(user.hasAgreedWithAup(institution));

        Institution other = new Institution();
        other.setId(2L);
        other.setAupUrl("https://aup");
        other.setAupVersion(1);
        assertFalse(user.hasAgreedWithAup(other));


    }
}