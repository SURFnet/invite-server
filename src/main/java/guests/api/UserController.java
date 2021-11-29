package guests.api;

import guests.domain.Institution;
import guests.domain.User;
import guests.repository.InstitutionRepository;
import guests.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/guests/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class UserController {

    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<User> get(User user) {
        return ResponseEntity.ok(user);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(User user) {
        userRepository.delete(user);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
