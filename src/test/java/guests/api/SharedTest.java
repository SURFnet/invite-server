package guests.api;

import guests.domain.*;
import guests.exception.UserRestrictionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static guests.api.Shared.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SharedTest {

    @Test
    void verifyAuthorities() {
        User user = getUser(Authority.GUEST);
        Institution institution1 = user.getInstitutionMemberships().iterator().next().getInstitution();
        assertThrows(UserRestrictionException.class, () -> verifyAuthority(user, institution1.getId(), Authority.INVITER));

        verifyAuthority(user, institution1.getId(), Authority.GUEST);
    }

    @Test
    void superAdminIsAllowedAll() {
        User authenticatedUser = getUser(Authority.SUPER_ADMIN);
        User subject = getUser(Authority.SUPER_ADMIN);
        viewOtherUserAllowed(authenticatedUser, subject);
        deleteOtherUserAllowed(authenticatedUser, subject);

        InstitutionMembership institutionMembershipSubject = subject.getInstitutionMemberships().iterator().next();
        UserRole userRole = new UserRole(new Role("role", new Application(institutionMembershipSubject.getInstitution())), null);
        subject.addUserRole(userRole);

        deleteUserRoleAllowed(authenticatedUser, userRole);
        deleteInstitutionMembershipAllowed(authenticatedUser, institutionMembershipSubject);
    }

    @Test
    void userIsAllowedToDeleteOwnRoleInstitutionMembership() {
        User user = getUser(Authority.GUEST);
        InstitutionMembership institutionMembership = user.getInstitutionMemberships().iterator().next();
        user.addUserRole(new UserRole(new Role("role", new Application(institutionMembership.getInstitution())), Instant.now()));
        deleteUserRoleAllowed(user, user.getRoles().iterator().next());
        deleteInstitutionMembershipAllowed(user, institutionMembership);
    }

    @Test
    void viewOtherUserNotAllowed() {
        User authenticatedUser = getUser(Authority.INVITER);
        User subject = getUser(Authority.GUEST);
        subject.setId(99L);

        viewOtherUserAllowed(authenticatedUser, subject);

        //ensure mismatch in InstitutionMembership.Institution
        subject.getInstitutionMemberships().iterator().next().getInstitution().setId(99L);
        assertThrows(UserRestrictionException.class, () -> viewOtherUserAllowed(authenticatedUser, subject));
    }

    @Test
    void deleteOtherUserNotAllowed() {
        User authenticatedUser = getUser(Authority.INVITER);
        User subject = getUser(Authority.GUEST);
        subject.setId(99L);

        viewOtherUserAllowed(authenticatedUser, subject);

        //ensure mismatch in InstitutionMembership.Institution
        subject.getInstitutionMemberships().iterator().next().getInstitution().setId(99L);
        assertThrows(UserRestrictionException.class, () -> viewOtherUserAllowed(authenticatedUser, subject));
    }

    @Test
    void deleteRoleNotAllowed() {
        User authenticatedUser = getUser(Authority.INSTITUTION_ADMINISTRATOR);
        User subject = getUser(Authority.INVITER);
        subject.setId(9L);
        InstitutionMembership institutionMembershipSubject = subject.getInstitutionMemberships().iterator().next();
        Application application = new Application(institutionMembershipSubject.getInstitution());
        Role role = new Role("name", application);
        UserRole userRole = new UserRole(role, Instant.now());
        subject.addUserRole(userRole);
        deleteUserRoleAllowed(authenticatedUser, userRole);
        deleteInstitutionMembershipAllowed(authenticatedUser, institutionMembershipSubject);

        //ensure mismatch in required authority
        authenticatedUser.getInstitutionMemberships().iterator().next().setAuthority(Authority.INVITER);
        assertThrows(UserRestrictionException.class, () -> deleteUserRoleAllowed(authenticatedUser, userRole));
        assertThrows(UserRestrictionException.class, () -> deleteInstitutionMembershipAllowed(authenticatedUser, institutionMembershipSubject));

        //ensure mismatch in InstitutionMembership.Institution
        authenticatedUser.getInstitutionMemberships().iterator().next().setAuthority(Authority.INSTITUTION_ADMINISTRATOR);
        institutionMembershipSubject.getInstitution().setId(99L);
        assertThrows(UserRestrictionException.class, () -> deleteUserRoleAllowed(authenticatedUser, userRole));
        assertThrows(UserRestrictionException.class, () -> deleteInstitutionMembershipAllowed(authenticatedUser, institutionMembershipSubject));
    }

    private User getUser(Authority authority) {
        User user = new User();
        user.setId(1L);
        Institution institution = new Institution();
        institution.setId(1L);
        user.addMembership(new InstitutionMembership(authority, institution));
        return user;
    }
}