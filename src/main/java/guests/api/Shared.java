package guests.api;

import guests.domain.Application;
import guests.domain.Institution;
import guests.domain.ObjectExists;
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

}
