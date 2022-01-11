package guests.api;

import guests.domain.Authority;
import guests.domain.ObjectExists;
import guests.domain.User;
import guests.exception.UserRestrictionException;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;
import java.util.stream.Collectors;

public class Shared {

    public static <V> ResponseEntity<Map<String, Boolean>> doesExists(@RequestBody ObjectExists objectExists, Optional<V> optional) {
        Boolean exists = optional.map(institution -> !objectExists.isExistingObject()).orElse(false);
        return ResponseEntity.ok(Collections.singletonMap("exists", exists));
    }

    public static <M> List<M> unProxy(Collection<M> objects, Class<M> clazz) {
        return objects.stream().map(obj -> Hibernate.unproxy(obj, clazz)).collect(Collectors.toList());
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
            if (!authority.isAllowed(required)) {
                throw userRestrictedException(user, institutionId);
            }
        }
    }

    public static void verifyAuthority(User authenticatedUser, User subject, Authority required) {
        if (!authenticatedUser.isSuperAdmin()) {
            Set<Long> institutionIdentifiers = subject.getInstitutionMemberships().stream()
                    .map(membership -> membership.getInstitution().getId()).collect(Collectors.toSet());
            if (authenticatedUser.getInstitutionMemberships().stream()
                    .noneMatch(membership -> institutionIdentifiers.contains(membership.getInstitution().getId())
                            && membership.getAuthority().isAllowed(required))) {
                throw userRestrictedException(authenticatedUser, institutionIdentifiers.iterator().next());
            }
        }
    }

    public static void verifyAuthorityForSubject(User authenticatedUser, User subject) {
        if (!authenticatedUser.isSuperAdmin()) {
            Set<Long> institutionIdentifiers = subject.getInstitutionMemberships().stream()
                    .map(membership -> membership.getInstitution().getId()).collect(Collectors.toSet());
            Set<Authority> subjectAuthorities = subject.getInstitutionMemberships().stream()
                    .map(membership -> membership.getAuthority()).collect(Collectors.toSet());
            if (authenticatedUser.getInstitutionMemberships().stream()
                    .noneMatch(membership -> institutionIdentifiers.contains(membership.getInstitution().getId())
                            && membership.getAuthority().isAllowedForAll(subjectAuthorities))) {
                throw userRestrictedException(authenticatedUser, institutionIdentifiers.iterator().next());
            }
        }
    }

    public static UserRestrictionException userRestrictedException(User authenticatedUser, Long institutionId) {
        return new UserRestrictionException(String.format("User %s is not allowed to act for institution %s",
                authenticatedUser.getEduPersonPrincipalName(), institutionId));
    }

    public static ResponseEntity<Map<String, Integer>> createdResponse() {
        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("status", 201));
    }

}
