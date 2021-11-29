package guests.api;

import guests.domain.Application;
import guests.domain.Institution;
import guests.repository.InstitutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
