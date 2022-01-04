package guests.api;

import guests.domain.*;
import guests.exception.UserRestrictionException;
import org.hibernate.Hibernate;
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

    public static void verifyUser(User authenticatedUser, User subject) {
        if (!authenticatedUser.isSuperAdmin()) {
            Set<Long> institutionIdentifiers = authenticatedUser.getMemberships().stream().map(membership -> membership.getInstitution().getId()).collect(Collectors.toSet());
            if (subject.getMemberships().stream().noneMatch(membership -> institutionIdentifiers.contains(membership.getInstitution().getId()))) {
                throw userRestrictedException(authenticatedUser, institutionIdentifiers.iterator().next());
            }
        }
    }

    private static UserRestrictionException userRestrictedException(User authenticatedUser, Long institutionId) {
        return new UserRestrictionException(String.format("User %s is not allowed to act for institution %s",
                authenticatedUser.getEduPersonPrincipalName(), institutionId));
    }

    public static void verifySuperUser(User user) {
        if (!user.isSuperAdmin()) {
            throw new UserRestrictionException("Authority mismatch");
        }
    }

    public static void verifyAuthority(User user, Long institutionId, Authority required) {
        Authority authority = user.authorityByInstitution(institutionId).orElseThrow(() -> userRestrictedException(user, institutionId));
        if (!authority.equals(Authority.SUPER_ADMIN) && !authority.isAllowed(required)) {
            throw new UserRestrictionException("Authority mismatch");
        }
        if (authority.equals(Authority.INVITER) && !required.equals(Authority.GUEST)) {
            throw new UserRestrictionException("Authority mismatch");
        }
    }


    public static void verifyAuthority(User authenticatedUser, User subject, Authority required) {
        if (!authenticatedUser.isSuperAdmin()) {
            Set<Long> institutionIdentifiers = authenticatedUser.getMemberships().stream().map(membership -> membership.getInstitution().getId()).collect(Collectors.toSet());
            if (subject.getMemberships().stream()
                    .noneMatch(membership -> institutionIdentifiers.contains(membership.getInstitution().getId())
                            && membership.getAuthority().isAllowed(required))) {
                throw userRestrictedException(authenticatedUser, institutionIdentifiers.iterator().next());
            }
        }
    }
}
