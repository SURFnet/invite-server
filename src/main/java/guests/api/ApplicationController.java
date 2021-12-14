package guests.api;

import guests.domain.Application;
import guests.domain.Institution;
import guests.domain.ObjectExists;
import guests.domain.User;
import guests.exception.NotFoundException;
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
        List<Application> applications = applicationRepository.findByRoles_IdIn(roleIdentifiers).stream()
                .map(application -> Hibernate.unproxy(application, Application.class)).collect(Collectors.toList());
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> getById(@PathVariable("id") Long id) {
        Application application = applicationRepository.findById(id).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(application);
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Application> save(@RequestBody Application application) {
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
