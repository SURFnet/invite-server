package guests.api;

import guests.domain.Authority;
import guests.domain.Institution;
import guests.domain.ObjectExists;
import guests.domain.User;
import guests.exception.NotFoundException;
import guests.repository.InstitutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static guests.api.Shared.*;

@RestController
@RequestMapping(value = "/guests/api/institutions", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class InstitutionController {

    private final InstitutionRepository institutionRepository;

    @Autowired
    public InstitutionController(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @GetMapping
    public ResponseEntity<List<Institution>> get(User user) {
        verifySuperUser(user);
        return ResponseEntity.ok(institutionRepository.findAll());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Institution>> mine(User user) {
        return ResponseEntity.ok(institutionRepository.findByInstitutionMemberships_user_id(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Institution> getById(User user, @PathVariable("id") Long id) {
        verifyUser(user, id);
        Institution institution = institutionRepository.findById(id).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(institution);
    }

    @PostMapping("entity-id-exists")
    public ResponseEntity<Map<String, Boolean>> entityIdExists(User user, @RequestBody ObjectExists objectExists) {
        verifySuperUser(user);
        Optional<Institution> optionalInstitution = institutionRepository.findByEntityIdIgnoreCase(objectExists.getUniqueAttribute());
        return doesExists(objectExists, optionalInstitution);
    }

    @PostMapping("schac-home-exists")
    public ResponseEntity<Map<String, Boolean>> schacHomeExists(User user, @RequestBody ObjectExists objectExists) {
        verifySuperUser(user);
        Optional<Institution> optionalInstitution = institutionRepository.findByHomeInstitutionIgnoreCase(objectExists.getUniqueAttribute());
        return doesExists(objectExists, optionalInstitution);
    }

    @PostMapping
    public ResponseEntity<Institution> save(User user, @RequestBody Institution institution) {
        verifySuperUser(user);
        institution.invariantAupVersion(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(institutionRepository.save(institution));
    }

    @PutMapping
    public ResponseEntity<Institution> update(User user, @RequestBody Institution institution) {
        verifyAuthority(user, institution.getId(), Authority.INSTITUTION_ADMINISTRATOR);
        Institution institutionFromDb = institutionRepository.findById(institution.getId()).orElseThrow(NotFoundException::new);
        if (!user.isSuperAdmin()) {
            institution.setHomeInstitution(institutionFromDb.getHomeInstitution());
            institution.setEntityId(institutionFromDb.getEntityId());
        }
        institution.invariantAupVersion(false);
        return ResponseEntity.status(HttpStatus.CREATED).body(institutionRepository.save(institution));
    }

    @PutMapping("increment-aup/{id}")
    public ResponseEntity<Map<String, Integer>> incrementAup(User authenticatedUser, @PathVariable("id") Long id) {
        Institution institution = institutionRepository.findById(id).orElseThrow(NotFoundException::new);
        verifyAuthority(authenticatedUser, id, Authority.INSTITUTION_ADMINISTRATOR);
        institution.incrementAup();
        institutionRepository.save(institution);
        return createdResponse();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Integer>> delete(User user, @PathVariable("id") Long id) {
        verifySuperUser(user);
        institutionRepository.deleteById(id);
        return createdResponse();
    }


}
