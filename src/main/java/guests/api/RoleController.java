package guests.api;

import guests.domain.*;
import guests.exception.NotFoundException;
import guests.repository.ApplicationRepository;
import guests.repository.RoleRepository;
import guests.scim.SCIMService;
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

import static guests.api.UserPermissions.*;

import static guests.api.Shared.*;

@RestController
@RequestMapping(value = "/api/v1/roles", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class RoleController {

    private final RoleRepository roleRepository;
    private final ApplicationRepository applicationRepository;
    private final SCIMService scimService;

    @Autowired
    public RoleController(RoleRepository roleRepository, ApplicationRepository applicationRepository, SCIMService scimService) {
        this.roleRepository = roleRepository;
        this.applicationRepository = applicationRepository;
        this.scimService = scimService;
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<Role>> rolesByInstitution(@PathVariable("institutionId") Long institutionId, User user) {
        verifyUser(user, institutionId);
        List<Role> roles = roleRepository.findByApplication_institution_id(institutionId);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<Role>> rolesByApplication(@PathVariable("applicationId") Long applicationId, User user) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(NotFoundException::new);
        verifyUser(user, application.getInstitution().getId());

        List<Role> roles = roleRepository.findByApplication_id(applicationId);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getById(User user, @PathVariable("id") Long id) {
        Role role = roleRepository.findById(id).orElseThrow(NotFoundException::new);
        verifyAuthority(user, role.getApplication().getInstitution().getId(), Authority.INSTITUTION_ADMINISTRATOR);
        return ResponseEntity.ok(role);
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Role> save(User user, @RequestBody Role role) {
        this.restrictUser(user, role);
        boolean isTransientRole = role.getId() == null;
        role = roleRepository.save(role);
        if (isTransientRole) {
            role.setApplication(applicationRepository.findById(role.getApplication().getId()).get());
            scimService.newRoleRequest(role);
        } else {
            scimService.updateRoleRequest(role);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }

    @PostMapping("name-exists")
    public ResponseEntity<Map<String, Boolean>> namExists(User authenticatedUser, @RequestBody RoleExists roleExists) {
        Application application = applicationRepository.findById(roleExists.getApplicationId()).orElseThrow(NotFoundException::new);
        verifyAuthority(authenticatedUser, application.getInstitution().getId(), Authority.INSTITUTION_ADMINISTRATOR);

        Optional<Role> optional = roleRepository.findByApplication_idAndNameIgnoreCase(roleExists.getApplicationId(), roleExists.getUniqueAttribute());
        return doesExists(roleExists, optional);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Integer>> delete(User authenticatedUser, @PathVariable("id") Long id) {
        Role role = roleRepository.findById(id).get();
        this.restrictUser(authenticatedUser, role);
        roleRepository.delete(role);
        scimService.deleteRolesRequest(role);
        return createdResponse();
    }

    private void restrictUser(User user, Role role) throws AuthenticationException {
        Application application = applicationRepository.findById(role.getApplication().getId()).orElseThrow(NotFoundException::new);
        verifyAuthority(user, application.getInstitution().getId(), Authority.INSTITUTION_ADMINISTRATOR);
    }
}
