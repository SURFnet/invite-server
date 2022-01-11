package guests.api;

import guests.domain.*;
import guests.exception.NotFoundException;
import guests.repository.ApplicationRepository;
import guests.repository.UserRepository;
import guests.scim.SCIMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static guests.api.Shared.*;

@RestController
@RequestMapping(value = "/guests/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class UserController {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final SCIMService scimService;

    @Autowired
    public UserController(UserRepository userRepository, ApplicationRepository applicationRepository, SCIMService scimService) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.scimService = scimService;
    }

    @GetMapping("me")
    public ResponseEntity<User> me(User authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.getId()).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(user);
    }

    @GetMapping("{userId}")
    public ResponseEntity<User> other(User authenticatedUser, @PathVariable("userId") Long userId) {
        User other = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        verifyAuthorityForSubject(authenticatedUser, other);
        return ResponseEntity.ok(other);
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<User>> getByInstitution(User user, @PathVariable("institutionId") Long institutionId) {
        verifyUser(user, institutionId);
        return ResponseEntity.ok(userRepository.findByInstitutionMemberships_Institution_id(institutionId));
    }

    @GetMapping("/emails/{institutionId}")
    public ResponseEntity<List<Map<String, String>>> emailsByInstitution(User user, @PathVariable("institutionId") Long institutionId) {
        verifyUser(user, institutionId);
        return ResponseEntity.ok(userRepository.findEmailAndNameByInstitution_id(institutionId));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<User>> getByApplication(User user, @PathVariable("applicationId") Long applicationId) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(NotFoundException::new);
        verifyUser(user, application.getInstitution().getId());
        return ResponseEntity.ok(userRepository.findByRoles_role_application_id(applicationId));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Integer>> delete(User user) {
        doDeleteUserAndUpdateScim(user);
        return createdResponse();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Integer>> deleteOther(User authenticatedUser, @PathVariable("userId") Long userId) {
        User subject = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        verifyAuthorityForSubject(authenticatedUser, subject);
        doDeleteUserAndUpdateScim(subject);
        return createdResponse();
    }

    private void doDeleteUserAndUpdateScim(User subject) {
        userRepository.delete(subject);
        scimService.deleteUserRequest(subject);
        Collection<Role> roles = subject.getRoles().stream().map(UserRole::getRole).collect(Collectors.toMap(Role::getId, role -> role)).values();
        roles.forEach(role -> {
            List<User> users = userRepository.findByRoles_role_id(role.getId());
            scimService.updateRoleRequest(role, users);
        });
    }

}
