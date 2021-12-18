package guests.api;

import guests.domain.Application;
import guests.domain.Authority;
import guests.domain.User;
import guests.exception.NotFoundException;
import guests.exception.UserRestrictionException;
import guests.repository.ApplicationRepository;
import guests.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static guests.api.Shared.verifyUser;

@RestController
@RequestMapping(value = "/guests/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class UserController {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    @GetMapping("me")
    public ResponseEntity<User> me(User authenticatedUser) {
        Optional<User> userOptional = userRepository.findById(authenticatedUser.getId());
        User user = userOptional.orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(user);
    }

    @Autowired
    public UserController(UserRepository userRepository, ApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<User>> getByInstitution(@PathVariable("institutionId") Long institutionId, User user) {
        verifyUser(user, institutionId);
        return ResponseEntity.ok(userRepository.findByInstitution_id(institutionId));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<User>> getyApplication(@PathVariable("applicationId") Long applicationId, User user) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(NotFoundException::new);
        verifyUser(user, application.getInstitution().getId());
        return ResponseEntity.ok(userRepository.findByRoles_role_application_id(applicationId));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(User user) {
        userRepository.delete(user);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
