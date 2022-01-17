package guests.api;

import guests.domain.Application;
import guests.domain.ApplicationExists;
import guests.domain.Authority;
import guests.domain.User;
import guests.exception.NotFoundException;
import guests.repository.ApplicationRepository;
import guests.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static guests.api.Shared.*;
import static guests.api.UserPermissions.*;

@RestController
@RequestMapping(value = "/api/v1/applications", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Autowired
    public ApplicationController(ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/user")
    public ResponseEntity<List<Application>> getForUser(User authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.getId()).orElseThrow(NotFoundException::new);
        List<Long> roleIdentifiers = user.getRoles().stream().map(role -> role.getRole().getId()).collect(Collectors.toList());
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
    public ResponseEntity<Application> save(User authenticatedUser, @RequestBody Application application) {
        verifyAuthority(authenticatedUser, application.getInstitution().getId(), Authority.INSTITUTION_ADMINISTRATOR);
        application.validateProvisioning();
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationRepository.save(application));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Integer>> delete(User authenticatedUser, @PathVariable("id") Long id) {
        Application application = applicationRepository.findById(id).orElseThrow(NotFoundException::new);
        verifyAuthority(authenticatedUser, application.getInstitution().getId(), Authority.INSTITUTION_ADMINISTRATOR);
        applicationRepository.delete(application);
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
