package guests.api;

import guests.domain.Application;
import guests.domain.ApplicationExists;
import guests.domain.Authority;
import guests.domain.User;
import guests.exception.NotAllowedException;
import guests.exception.NotFoundException;
import guests.repository.ApplicationRepository;
import guests.repository.UserRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static guests.api.Shared.*;
import static guests.api.UserPermissions.verifyAuthority;

@RestController
@RequestMapping(value = "/api/v1/applications", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class ApplicationController {

    private static final Log LOG = LogFactory.getLog(ApplicationController.class);

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Autowired
    public ApplicationController(ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/user-count/{applicationId}")
    public ResponseEntity<Long> userCount(@PathVariable("applicationId") Long applicationId) {
        return ResponseEntity.ok(applicationRepository.countUsers(applicationId));
    }

    @GetMapping("/user")
    public ResponseEntity<List<Application>> getForUser(User authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.getId()).orElseThrow(NotFoundException::new);
        List<Long> roleIdentifiers = user.getUserRoles().stream().map(role -> role.getRole().getId()).collect(Collectors.toList());
        return ResponseEntity.ok(unProxy(applicationRepository.findByRoles_IdIn(roleIdentifiers), Application.class));
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<Application>> getForInstitution(User authenticatedUser, @PathVariable("institutionId") Long institutionId) {
        verifyAuthority(authenticatedUser, institutionId, Authority.INVITER);
        return ResponseEntity.ok(applicationRepository.findByInstitution_id(institutionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> getById(User authenticatedUser, @PathVariable("id") Long id) {
        Application application = applicationRepository.findById(id).orElseThrow(NotFoundException::new);
        verifyAuthority(authenticatedUser, application.getInstitution().getId(), Authority.INSTITUTION_ADMINISTRATOR);
        return ResponseEntity.ok(application);
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Application> save(HttpServletRequest request, User authenticatedUser, @RequestBody Application application) {
        verifyAuthority(authenticatedUser, application.getInstitution().getId(), Authority.INSTITUTION_ADMINISTRATOR);
        application.validateProvisioning();

        LOG.debug(String.format("%s application %s by user %s",
                request.getMethod().equalsIgnoreCase("post") ? "Creating" : "Updating",
                application.getName(),
                authenticatedUser.getName()));

        return ResponseEntity.status(HttpStatus.CREATED).body(applicationRepository.save(application));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Integer>> delete(User authenticatedUser, @PathVariable("id") Long id) {
        Application application = applicationRepository.findById(id).orElseThrow(NotFoundException::new);

        verifyAuthority(authenticatedUser, application.getInstitution().getId(), Authority.INSTITUTION_ADMINISTRATOR);

        Long countUsers = applicationRepository.countUsers(application.getId());
        if (countUsers > 0) {
            throw new NotAllowedException(String.format("Application %s can not be deleted as there are %s active users",
                    application.getName(),
                    countUsers));
        }

        applicationRepository.delete(application);

        LOG.debug(String.format("Deleting application %s by user %s",
                application.getName(),
                authenticatedUser.getName()));

        return createdResponse();
    }

    @PostMapping("entity-id-exists")
    public ResponseEntity<Map<String, Boolean>> entityIdExists(User authenticatedUser, @RequestBody ApplicationExists applicationExists) {
        verifyAuthority(authenticatedUser, applicationExists.getInstitutionId(), Authority.INSTITUTION_ADMINISTRATOR);
        Optional<Application> optionalApplication = applicationRepository.findByInstitution_idAndEntityIdIgnoreCase(
                applicationExists.getInstitutionId(), applicationExists.getUniqueAttribute());
        return doesExists(applicationExists, optionalApplication);
    }

}
