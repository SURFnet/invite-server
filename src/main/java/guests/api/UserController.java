package guests.api;

import guests.domain.Application;
import guests.domain.Authority;
import guests.domain.User;
import guests.exception.NotFoundException;
import guests.repository.ApplicationRepository;
import guests.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static guests.api.Shared.*;

@RestController
@RequestMapping(value = "/guests/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class UserController {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    @Autowired
    public UserController(UserRepository userRepository, ApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("me")
    public ResponseEntity<User> me(User authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.getId()).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(user);
    }

    @GetMapping("{userId}")
    public ResponseEntity<User> other(User authenticatedUser, @PathVariable("userId") Long userId) {
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        verifyAuthority(authenticatedUser, user, Authority.INSTITUTION_ADMINISTRATOR);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<User>> getByInstitution(User user, @PathVariable("institutionId") Long institutionId) {
        verifyUser(user, institutionId);
        return ResponseEntity.ok(userRepository.findByInstitutionMemberships_Institution_id(institutionId));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<User>> getByApplication(User user, @PathVariable("applicationId") Long applicationId) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(NotFoundException::new);
        verifyUser(user, application.getInstitution().getId());
        return ResponseEntity.ok(userRepository.findByRoles_role_application_id(applicationId));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Integer>> delete(User user) {
        userRepository.delete(user);
        return createdResponse();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Integer>> deleteOther(User authenticatedUser, @PathVariable("userId") Long userId) {
        User subject = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        verifyAuthority(authenticatedUser, subject, Authority.INSTITUTION_ADMINISTRATOR);
        userRepository.delete(subject);
        return createdResponse();
    }

}
