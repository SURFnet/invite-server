package guests.api;

import guests.domain.*;
import guests.exception.NotFoundException;
import guests.exception.UserRestrictionException;
import guests.repository.ApplicationRepository;
import guests.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static guests.api.Shared.doesExists;

@RestController
@RequestMapping(value = "/guests/api/roles", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class RoleController {

    private final RoleRepository roleRepository;
    private final ApplicationRepository applicationRepository;

    @Autowired
    public RoleController(RoleRepository roleRepository, ApplicationRepository applicationRepository) {
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping()
    public ResponseEntity<List<Role>> roles(User user) {
        List<Role> roles = roleRepository.findByApplication_institution(user.getInstitution());
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getById(@PathVariable("id") Long id) {
        Role role = roleRepository.findById(id).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(role);
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Role> save(User user, @RequestBody Role role) {
        this.restrictUser(user, role);
        role = roleRepository.save(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }

    @PostMapping("name-exists")
    public ResponseEntity<Map<String, Boolean>> namExists(@RequestBody RoleExists roleExists) {
        Optional<Role> optional = roleRepository.findByApplication_idAndNameIgnoreCase(roleExists.getApplicationId(), roleExists.getUniqueAttribute());
        return doesExists(roleExists, optional);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(User user, @PathVariable("id") Long id) {
        Role role = roleRepository.findById(id).get();
        this.restrictUser(user, role);
        roleRepository.delete(role);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private void restrictUser(User user, Role role) throws AuthenticationException {
        Application application = applicationRepository.findById(role.getApplication().getId()).orElseThrow(NotFoundException::new);
        Institution institution = application.getInstitution();
        if (!user.getAuthority().equals(Authority.SUPER_ADMIN) && !institution.getId().equals(user.getInstitution().getId())) {
            throw new UserRestrictionException("Application mismatch");
        }
    }
}
