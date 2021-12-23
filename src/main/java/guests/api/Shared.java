package guests.api;

import guests.domain.*;
import guests.exception.UserRestrictionException;
import org.hibernate.Hibernate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Shared {

    public static <V> ResponseEntity<Map<String, Boolean>> doesExists(@RequestBody ObjectExists objectExists, Optional<V> optional) {
        Boolean exists = optional.map(institution -> !objectExists.isExistingObject()).orElse(false);
        return ResponseEntity.ok(Collections.singletonMap("exists", exists));
    }

    public static <M> List<M> unProxy(List<M> objects, Class<M> clazz) {
        return objects.stream().map(obj -> Hibernate.unproxy(obj, clazz)).collect(Collectors.toList());
    }

    public static void verifyUser(User authenticatedUser, Long institutionId) {
        if (authenticatedUser.getAuthority().equals(Authority.INSTITUTION_ADMINISTRATOR) &&
                !authenticatedUser.getInstitution().getId().equals(institutionId)) {
            throw new UserRestrictionException(String.format("User %s is not allowed to act for institution %s",
                    authenticatedUser.getEduPersonPrincipalName(), institutionId));
        }
    }

    public static void verifyAuthority(User user, Authority required) {
        if (!user.getAuthority().isAllowed(required)) {
            throw new UserRestrictionException("Authority mismatch");
        }
    }
}
