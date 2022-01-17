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


    @Test
    void removeUserRole() {
        User user = new User();
        UserRole role = new UserRole();
        role.setId(1L);
        user.addUserRole(role);
        UserRole otherRole = new UserRole();
        otherRole.setId(2L);

        user.removeUserRole(otherRole);
        assertEquals(1, user.getRoles().size());

        user.addUserRole(otherRole);
        user.removeUserRole(otherRole);
        assertEquals(1, user.getRoles().size());
    }

    @Test
    void removeMemberShip() {
        User user = new User();
        InstitutionMembership membership = new InstitutionMembership();
        membership.setId(1L);
        user.addMembership(membership);
        InstitutionMembership otherMembership = new InstitutionMembership();
        otherMembership.setId(2L);

        user.removeMembership(otherMembership);
        assertEquals(1, user.getInstitutionMemberships().size());

        user.addMembership(otherMembership);
        user.removeMembership(otherMembership);
        assertEquals(1, user.getInstitutionMemberships().size());
    }

    @Test
    void removeOtherInstitutionData() {
        User user = new User();
        AtomicLong id = new AtomicLong();
        List<Application> apps = Arrays.asList(
                application("LMS", id, "Guests", "Admins"),
                application("Blackboard", id, "Students"),
                application("Canvas", id)
        );
        apps.forEach(app -> app.getRoles().forEach(role -> user.addUserRole(new UserRole(role, null))));
        apps.forEach(app -> user.addMembership(new InstitutionMembership(Authority.INVITER, app.getInstitution())));

        assertEquals(3, user.getRoles().size());

        user.removeOtherInstitutionData(apps.get(0).getInstitution().getId());
        assertEquals(2, user.getRoles().size());
        assertEquals(1, user.getInstitutionMemberships().size());

        User other = new User();
        apps.get(0).getRoles().forEach(role -> other.addUserRole(new UserRole(role, null)));
        other.addMembership(new InstitutionMembership(Authority.INVITER, apps.get(1) .getInstitution()));

        user.removeOtherInstitutionData(other);
        assertEquals(0, user.getRoles().size());
        assertEquals(0, user.getInstitutionMemberships().size());
    }

    private Application application(String name, AtomicLong id, String... roles) {
        Institution institution = new Institution();
        institution.setId(id.incrementAndGet());

        Application app = new Application();
        app.setName(name);
        app.setId(id.incrementAndGet());
        app.setInstitution(institution);

        Arrays.stream(roles).forEach(roleName -> {
            app.addRole(new Role(id.incrementAndGet(), roleName));
        });

        return app;
    }

}