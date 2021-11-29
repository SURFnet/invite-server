package guests.api;

import guests.domain.Application;
import guests.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/guests/api/applications", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class ApplicationController {

    private final ApplicationRepository repository;

    @Autowired
    public ApplicationController(ApplicationRepository applicationRepository) {
        this.repository = applicationRepository;
    }

    @GetMapping
    public ResponseEntity<List<Application>> get() {
        return ResponseEntity.ok(repository.findAll());
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Application> save(@RequestBody Application application) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(application));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        repository.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
