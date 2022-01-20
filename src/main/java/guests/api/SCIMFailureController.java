package guests.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import guests.domain.Authority;
import guests.domain.Role;
import guests.domain.SCIMFailure;
import guests.domain.User;
import guests.exception.NotFoundException;
import guests.repository.RoleRepository;
import guests.repository.SCIMFailureRepository;
import guests.repository.UserRepository;
import guests.scim.SCIMService;
import guests.scim.ThreadLocalSCIMFailureStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static guests.api.UserPermissions.*;

import static guests.api.Shared.*;

@RestController
@RequestMapping(value = "/api/v1/scim", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class SCIMFailureController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SCIMFailureRepository scimFailureRepository;
    private final SCIMService scimService;

    @Autowired
    public SCIMFailureController(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 SCIMFailureRepository scimFailureRepository,
                                 SCIMService scimService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.scimFailureRepository = scimFailureRepository;
        this.scimService = scimService;
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<SCIMFailure>> failures(User authenticatedUser, @PathVariable("institutionId") Long institutionId) {
        verifyAuthority(authenticatedUser, institutionId, Authority.INSTITUTION_ADMINISTRATOR);
        List<SCIMFailure> scimFailures = this.scimFailureRepository.findByApplication_institution_id(institutionId);
        return ResponseEntity.ok(scimFailures);
    }

    @GetMapping("/institution/{institutionId}/count")
    public ResponseEntity<Map<String, Long>> failuresCounts(User authenticatedUser, @PathVariable("institutionId") Long institutionId) {
        verifyAuthority(authenticatedUser, institutionId, Authority.INSTITUTION_ADMINISTRATOR);
        long count = this.scimFailureRepository.countByApplication_institution_id(institutionId);
        return ResponseEntity.ok(Collections.singletonMap("count", count));
    }

    @GetMapping("/id/{id}/{institutionId}")
    public ResponseEntity<SCIMFailure> failureById(User authenticatedUser,
                                                   @PathVariable("id") Long id,
                                                   @PathVariable("institutionId") Long institutionId) {
        SCIMFailure scimFailure = getScimFailure(authenticatedUser, id, institutionId);
        return ResponseEntity.ok(scimFailure);
    }

    @PutMapping("/id/{id}/{institutionId}")
    public ResponseEntity<Map<String, Integer>> resend(User authenticatedUser,
                                                       @PathVariable("id") Long id,
                                                       @PathVariable("institutionId") Long institutionId) throws JsonProcessingException {
        verifySuperUser(authenticatedUser);
        try {
            ThreadLocalSCIMFailureStrategy.startIgnoringFailures();
            SCIMFailure scimFailure = getScimFailure(authenticatedUser, id, institutionId);
            Optional<Serializable> serializableOptional = scimService.resendScimFailure(scimFailure);
            serializableOptional.ifPresent(serializable -> {
                if (serializable instanceof User user) {
                    userRepository.save(user);
                } else if (serializable instanceof Role role) {
                    roleRepository.save(role);
                }
            });
            //If there are no exceptions then we delete the failure
            this.scimFailureRepository.delete(scimFailure);
            return createdResponse();
        } finally {
            ThreadLocalSCIMFailureStrategy.stopIgnoringFailures();
        }

    }

    @DeleteMapping("/id/{id}/{institutionId}")
    public ResponseEntity<Map<String, Integer>> delete(User authenticatedUser,
                                                       @PathVariable("id") Long id,
                                                       @PathVariable("institutionId") Long institutionId) {
        SCIMFailure scimFailure = getScimFailure(authenticatedUser, id, institutionId);
        this.scimFailureRepository.delete(scimFailure);
        return createdResponse();
    }

    private SCIMFailure getScimFailure(User authenticatedUser, @PathVariable("id") Long id, @PathVariable("institutionId") Long institutionId) {
        verifyAuthority(authenticatedUser, institutionId, Authority.INSTITUTION_ADMINISTRATOR);
        SCIMFailure scimFailure = this.scimFailureRepository.findById(id).orElseThrow(NotFoundException::new);
        if (!authenticatedUser.isSuperAdmin() && !scimFailure.getApplication().getInstitution().getId().equals(institutionId)) {
            throw userRestrictedException(authenticatedUser, institutionId);
        }
        return scimFailure;
    }

}
