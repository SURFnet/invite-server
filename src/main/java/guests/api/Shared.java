package guests.api;

import guests.domain.Institution;
import guests.domain.ObjectExists;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class Shared {

    public static <V> ResponseEntity<Map<String, Boolean>> doesExists(@RequestBody ObjectExists objectExists, Optional<V> optional) {
        Boolean exists = optional.map(institution -> !objectExists.isExistingObject()).orElse(false);
        return ResponseEntity.ok(Collections.singletonMap("exists", exists));
    }

}
