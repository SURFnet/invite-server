package guests.api;

import guests.domain.Authority;
import guests.domain.User;
import guests.exception.NotFoundException;
import guests.exception.UserRestrictionException;
import guests.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/guests/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class UserController {

    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("me")
    public ResponseEntity<User> me(User authenticatedUser) {
        Optional<User> userOptional = userRepository.findById(authenticatedUser.getId());
        User user = userOptional.orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<User>> get(@PathVariable("institutionId") Long institutionId, User user) {
        if (!user.getAuthority().equals(Authority.SUPER_ADMIN) && !user.getInstitution().getId().equals(institutionId)) {
            throw new UserRestrictionException(String.format("User %s is only allowed to access users from %s",
                    user.getEduPersonPrincipalName(), user.getInstitution().getDisplayName()));
        }
        return ResponseEntity.ok(userRepository.findByInstitution_id(institutionId));
    }


    @DeleteMapping
    public ResponseEntity<Void> delete(User user) {
        userRepository.delete(user);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
