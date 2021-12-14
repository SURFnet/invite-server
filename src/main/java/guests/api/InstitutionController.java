package guests.api;

import guests.domain.Institution;
import guests.domain.ObjectExists;
import guests.exception.NotFoundException;
import guests.repository.InstitutionRepository;
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

import static guests.api.Shared.doesExists;

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
    public ResponseEntity<List<Institution>> get() {
        return ResponseEntity.ok(institutionRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Institution> getById(@PathVariable("id") Long id) {
        Institution institution = institutionRepository.findById(id).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(institution);
    }

    @PostMapping("entity-id-exists")
    public ResponseEntity<Map<String, Boolean>> entityIdExists(@RequestBody ObjectExists objectExists) {
        Optional<Institution> optionalInstitution = institutionRepository.findByEntityIdIgnoreCase(objectExists.getUniqueAttribute());
        return doesExists(objectExists, optionalInstitution);
    }

    @PostMapping("schac-home-exists")
    public ResponseEntity<Map<String, Boolean>> schacHomeExists(@RequestBody ObjectExists objectExists) {
        Optional<Institution> optionalInstitution = institutionRepository.findByHomeInstitutionIgnoreCase(objectExists.getUniqueAttribute());
        return doesExists(objectExists, optionalInstitution);
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Institution> save(@RequestBody Institution institution) {
        return ResponseEntity.status(HttpStatus.CREATED).body(institutionRepository.save(institution));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        institutionRepository.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
