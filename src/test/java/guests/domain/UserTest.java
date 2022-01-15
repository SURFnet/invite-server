package guests.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void userRolesPerApplication() {
        User user = new User();
        AtomicLong id = new AtomicLong();
        List<Application> apps = Arrays.asList(
                application("LMS", id, "Guests", "Admins"),
                application("Blackboard", id, "Students"),
                application("Canvas", id)
        );
        apps.forEach(app -> app.getRoles().forEach(role -> user.addUserRole(new UserRole(role, null))));

        Map<Application, List<UserRole>> applicationListMap = user.userRolesPerApplication();
        assertEquals(2, applicationListMap.size());
        applicationListMap.forEach((app, userRoles) ->
                assertEquals(app.getName().equals("LMS") ? 2 : 1, userRoles.size()));
    }

    @Test
    void userRolesPerApplicationEmpty() {
        User user = new User();
        user.addUserRole(new UserRole());
        user.addUserRole(new UserRole(new Role(), null));
        Map<Application, List<UserRole>> applicationListMap = user.userRolesPerApplication();
        assertEquals(0, applicationListMap.size());
    }

    private Application application(String name, AtomicLong id, String... roles) {
        Application app = new Application();
        app.setName(name);
        app.setId(id.incrementAndGet());

        Arrays.stream(roles).forEach(roleName -> {
            app.addRole(new Role(id.incrementAndGet(), roleName));
        });

        return app;
    }
}