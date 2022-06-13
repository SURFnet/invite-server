package guests.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import guests.domain.Application;
import guests.domain.InstitutionMembership;
import guests.domain.User;
import guests.domain.UserRole;
import guests.exception.NotFoundException;
import guests.repository.ApplicationRepository;
import guests.repository.UserRepository;
import guests.scim.OperationType;
import guests.scim.SCIMService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static guests.api.Shared.createdResponse;
import static guests.api.UserPermissions.*;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class UserController {

    private static final Log LOG = LogFactory.getLog(UserController.class);

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final SCIMService scimService;

    @Autowired
    public UserController(UserRepository userRepository,
                          ApplicationRepository applicationRepository,
                          SCIMService scimService) {
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

        viewOtherUserAllowed(authenticatedUser, other);
        removeOtherInstitutionData(authenticatedUser, other);

        return ResponseEntity.ok(other);
    }

    @GetMapping("/institution/{institutionId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<User>> getByInstitution(User authenticatedUser, @PathVariable("institutionId") Long institutionId) {
        verifyUser(authenticatedUser, institutionId);
        List<User> users = userRepository.findByInstitutionMemberships_Institution_id(institutionId);
        removeOtherInstitutionData(authenticatedUser, institutionId, users);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/emails/{institutionId}")
    public ResponseEntity<List<Map<String, String>>> emailsByInstitution(User user,
                                                                         @PathVariable("institutionId") Long institutionId) {
        verifyUser(user, institutionId);
        return ResponseEntity.ok(userRepository.findEmailAndNameByInstitution_id(institutionId));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<User>> getByApplication(User user, @PathVariable("applicationId") Long applicationId) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(NotFoundException::new);
        verifyUser(user, application.getInstitution().getId());
        return ResponseEntity.ok(userRepository.findByUserRoles_role_application_id(applicationId));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Integer>> delete(User authenticatedUser) {
        doDeleteUser(authenticatedUser);
        return createdResponse();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Integer>> deleteOther(User authenticatedUser,
                                                            @PathVariable("userId") Long userId) {
        User subject = userRepository.findById(userId).orElseThrow(NotFoundException::new);

        deleteOtherUserAllowed(authenticatedUser, subject);

        doDeleteUser(subject);
        return createdResponse();
    }

    @DeleteMapping("/role/{userId}/{userRoleId}")
    public ResponseEntity<Map<String, Integer>> deleteRoleForOther(User authenticatedUser,
                                                                   @PathVariable("userId") Long userId,
                                                                   @PathVariable("userRoleId") Long userRoleId) {
        User subject = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        UserRole userRole = subject.getUserRoles().stream()
                .filter(r -> r.getId().equals(userRoleId)).findFirst().orElseThrow(NotFoundException::new);

        deleteUserRoleAllowed(authenticatedUser, userRole);

        subject.removeUserRole(userRole);
        userRepository.save(subject);
        scimService.updateRoleRequest(userRole, OperationType.Remove);
        return createdResponse();
    }

    @DeleteMapping("/membership/{userId}/{membershipId}")
    public ResponseEntity<Map<String, Integer>> deleteMembershipForOther(User authenticatedUser,
                                                                         @PathVariable("userId") Long userId,
                                                                         @PathVariable("membershipId") Long membershipId) {
        User subject = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        InstitutionMembership institutionMembership = subject.getInstitutionMemberships().stream()
                .filter(m -> m.getId().equals(membershipId)).findFirst().orElseThrow(NotFoundException::new);

        deleteInstitutionMembershipAllowed(authenticatedUser, institutionMembership);

        Long institutionId = institutionMembership.getInstitution().getId();
        List<UserRole> userRoles = subject.getUserRoles().stream()
                .filter(userRole -> userRole.getRole().getApplication().getInstitution().getId().equals(institutionId))
                .collect(Collectors.toList());

        userRoles.forEach(subject::removeUserRole);
        subject.removeMembership(institutionMembership);

        LOG.debug(String.format("Deleting membership %s for user %s",
                institutionMembership.getAuthority(),
                subject.getName()));

        userRepository.save(subject);

        userRoles.forEach(userRole -> scimService.updateRoleRequest(userRole, OperationType.Remove));

        return createdResponse();
    }

    private void doDeleteUser(User subject) {
        scimService.deleteUserRequest(subject);
        userRepository.delete(subject);

        LOG.info(String.format("Deleting user %s", subject.getName()));
    }

    private void removeOtherInstitutionData(User authenticatedUser, Long institutionId, List<User> users) {
        if (!authenticatedUser.isSuperAdmin()) {
            users.forEach(user -> user.removeOtherInstitutionData(institutionId));
        }
    }

    private void removeOtherInstitutionData(User authenticatedUser, User user) {
        if (!authenticatedUser.isSuperAdmin()) {
            user.removeOtherInstitutionData(authenticatedUser);
        }
    }
}
