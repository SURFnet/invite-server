package guests.api;

import guests.domain.*;
import guests.exception.NotFoundException;
import guests.exception.UserRestrictionException;
import guests.repository.ApplicationRepository;
import guests.repository.UserRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static guests.api.Shared.doesExists;
import static guests.api.Shared.unProxy;

@RestController
@RequestMapping(value = "/guests/api/applications", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Autowired
    public ApplicationController(ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Application>> get() {
        return ResponseEntity.ok(applicationRepository.findAll());
    }

    @GetMapping("/user")
    public ResponseEntity<List<Application>> getForUser(User authenticatedUser) {
        Optional<User> userOptional = userRepository.findById(authenticatedUser.getId());
        User user = userOptional.orElseThrow(NotFoundException::new);
        List<Long> roleIdentifiers = user.getRoles().stream().map(role -> role.getRole().getId()).collect(Collectors.toList());
        return ResponseEntity.ok(unProxy(applicationRepository.findByRoles_IdIn(roleIdentifiers), Application.class));
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<Application>> getForInstitution(@PathVariable("institutionId") Long institutionId) {
        return ResponseEntity.ok(applicationRepository.findByInstitution_id(institutionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> getById(@PathVariable("id") Long id) {
        Application application = applicationRepository.findById(id).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(application);
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Application> save(User authenticatedUser, @RequestBody Application application) {
        if (authenticatedUser.getAuthority().equals(Authority.INSTITUTION_ADMINISTRATOR) &&
                !authenticatedUser.getInstitution().getId().equals(application.getInstitution().getId())) {
            throw new UserRestrictionException(String.format("User %s is not allowed to create an application for institution %s",
                    authenticatedUser.getEduPersonPrincipalName(), application.getInstitution().getDisplayName()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationRepository.save(application));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        applicationRepository.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("entity-id-exists")
    public ResponseEntity<Map<String, Boolean>> entityIdExists(@RequestBody ObjectExists objectExists) {
        Optional<Application> optionalApplication = applicationRepository.findByEntityIdIgnoreCase(objectExists.getUniqueAttribute());
        return doesExists(objectExists, optionalApplication);
    }

}
