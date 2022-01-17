package guests.api;

import guests.domain.*;
import guests.exception.UserRestrictionException;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;
import java.util.stream.Collectors;

public class Shared {

    private Shared() {
    }

    public static <V> ResponseEntity<Map<String, Boolean>> doesExists(@RequestBody ObjectExists objectExists, Optional<V> optional) {
        Boolean exists = optional.map(institution -> !objectExists.isExistingObject()).orElse(false);
        return ResponseEntity.ok(Collections.singletonMap("exists", exists));
    }

    public static <M> List<M> unProxy(Collection<M> objects, Class<M> clazz) {
        return objects.stream().map(obj -> Hibernate.unproxy(obj, clazz)).collect(Collectors.toList());
    }

    public static ResponseEntity<Map<String, Integer>> createdResponse() {
        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("status", 201));
    }

}
