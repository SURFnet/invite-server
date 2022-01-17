package guests.api;

import guests.domain.*;
import guests.exception.UserRestrictionException;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;
import java.util.stream.Collectors;

public class UserPermissions {

    private UserPermissions() {
    }

    public static void verifyUser(User authenticatedUser, Long institutionId) {
        if (!authenticatedUser.isSuperAdmin()) {
            authenticatedUser.authorityByInstitution(institutionId).orElseThrow(() -> userRestrictedException(authenticatedUser, institutionId));
        }
    }

    public static void verifySuperUser(User user) {
        if (!user.isSuperAdmin()) {
            throw new UserRestrictionException("Authority mismatch");
        }
    }

    public static void verifyAuthority(User user, Long institutionId, Authority required) {
        if (!user.isSuperAdmin()) {
            Authority authority = user.authorityByInstitution(institutionId).orElseThrow(() -> userRestrictedException(user, institutionId));
            if (!authority.hasEqualOrHigherRights(required)) {
                throw userRestrictedException(user, institutionId);
            }
        }
    }

    public static void viewOtherUserAllowed(User authenticatedUser, User subject) {
        if (!authenticatedUser.isSuperAdmin() && !authenticatedUser.getId().equals(subject.getId())) {
            boolean allowed = authenticatedUser.getInstitutionMemberships().stream()
                    .anyMatch(membership -> subject.getInstitutionMemberships().stream()
                            .anyMatch(subjectMembership -> subjectMembership.getInstitution().getId().equals(membership.getInstitution().getId()) &&
                                    membership.getAuthority().hasHigherRights(subjectMembership.getAuthority())));
            if (!allowed) {
                throw userRestrictedException(authenticatedUser, subject);
            }
        }
    }

    public static void deleteOtherUserAllowed(User authenticatedUser, User subject) {
        if (!authenticatedUser.isSuperAdmin() && !authenticatedUser.getId().equals(subject.getId())) {
            boolean allowed = subject.getInstitutionMemberships().stream()
                    .allMatch(subjectMembership -> authenticatedUser.getInstitutionMemberships().stream()
                            .anyMatch(membership -> membership.getInstitution().getId().equals(subjectMembership.getInstitution().getId()) &&
                                    membership.getAuthority().hasHigherRights(subjectMembership.getAuthority())));
            if (!allowed) {
                throw userRestrictedException(authenticatedUser, subject);
            }
        }
    }

    public static void deleteUserRoleAllowed(User authenticatedUser, UserRole subjectUserRole) {
        User subject = subjectUserRole.getUser();
        Long institutionId = subjectUserRole.getRole().getApplication().getInstitution().getId();
        Authority highestAuthority = subject.getInstitutionMemberships().stream()
                .map(InstitutionMembership::getAuthority)
                .max(Authority::compareRights)
                .orElse(Authority.GUEST);
        doDeleteUserRoleOrMembershipAllowed(authenticatedUser, subject, institutionId, highestAuthority);
    }

    public static void deleteInstitutionMembershipAllowed(User authenticatedUser, InstitutionMembership institutionMembership) {
        User subject = institutionMembership.getUser();
        Long institutionId = institutionMembership.getInstitution().getId();
        doDeleteUserRoleOrMembershipAllowed(authenticatedUser, subject, institutionId, institutionMembership.getAuthority());
    }

    public static UserRestrictionException userRestrictedException(User authenticatedUser, Long institutionId) {
        return new UserRestrictionException(String.format("User %s is not allowed to act for institution %s",
                authenticatedUser.getEduPersonPrincipalName(), institutionId));
    }

    public static UserRestrictionException userRestrictedException(User authenticatedUser, User subject) {
        return new UserRestrictionException(String.format("User %s is not allowed to act for user %s",
                authenticatedUser.getEduPersonPrincipalName(), subject.getEduPersonPrincipalName()));
    }

    private static void doDeleteUserRoleOrMembershipAllowed(User authenticatedUser,
                                                            User subject,
                                                            Long institutionId,
                                                            Authority authority) {
        if (!authenticatedUser.isSuperAdmin() && !authenticatedUser.getId().equals(subject.getId())) {
            boolean allowed = authenticatedUser.getInstitutionMemberships().stream()
                    .anyMatch(membership -> membership.getInstitution().getId().equals(institutionId) &&
                            membership.getAuthority().hasHigherRights(authority));
            if (!allowed) {
                throw userRestrictedException(authenticatedUser, subject);
            }
        }
    }

}
