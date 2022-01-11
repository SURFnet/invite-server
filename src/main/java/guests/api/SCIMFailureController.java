package guests.api;

import guests.domain.Authority;
import guests.domain.SCIMFailure;
import guests.domain.User;
import guests.exception.NotFoundException;
import guests.repository.SCIMFailureRepository;
import guests.scim.SCIMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static guests.api.Shared.*;

@RestController
@RequestMapping(value = "/guests/api/scim", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class SCIMFailureController {

    private final SCIMFailureRepository scimFailureRepository;
    private final SCIMService scimService;

    @Autowired
    public SCIMFailureController(SCIMFailureRepository scimFailureRepository, SCIMService scimService) {
        this.scimFailureRepository = scimFailureRepository;
        this.scimService = scimService;
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<SCIMFailure>> failures(User authenticatedUser, @PathVariable("institutionId") Long institutionId) {
        verifyAuthority(authenticatedUser, institutionId, Authority.INSTITUTION_ADMINISTRATOR);
        List<SCIMFailure> scimFailures = this.scimFailureRepository.findByApplication_institution_id(institutionId);
        return ResponseEntity.ok(scimFailures);
    }

    @GetMapping("/id/{id}/{institutionId}")
    public ResponseEntity<SCIMFailure> failureById(User authenticatedUser,
                                                   @PathVariable("id") Long id,
                                                   @PathVariable("institutionId") Long institutionId) {
        verifyAuthority(authenticatedUser, institutionId, Authority.INSTITUTION_ADMINISTRATOR);
        SCIMFailure scimFailure = this.scimFailureRepository.findById(id).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(scimFailure);
    }

    @PutMapping("/id/{id}/{institutionId}")
    public ResponseEntity<Map<String, Integer>> resend(User authenticatedUser,
                                                       @PathVariable("id") Long id,
                                                       @PathVariable("institutionId") Long institutionId) {
        SCIMFailure scimFailure = getScimFailure(authenticatedUser, id, institutionId);
        this.scimFailureRepository.delete(scimFailure);
        return createdResponse();
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
        if (!scimFailure.getApplication().getInstitution().getId().equals(institutionId)) {
            throw userRestrictedException(authenticatedUser, institutionId);
        }
        return scimFailure;
    }

}
