package guests.api;

import guests.domain.*;
import guests.exception.NotFoundException;
import guests.repository.InstitutionRepository;
import guests.repository.UserRepository;
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
@RequestMapping(value = "/guests/api/aups", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class AupController {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;

    @Autowired
    public AupController(UserRepository userRepository, InstitutionRepository institutionRepository) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
    }

    @PutMapping
    public ResponseEntity<Map<String, Integer>> aups(User authenticatedUser, @RequestBody List<Long> institutionIdentifiers) {
        User user = userRepository.findById(authenticatedUser.getId()).orElseThrow(NotFoundException::new);
        institutionIdentifiers.forEach(institutionIdentifier -> {
            verifyUser(user, institutionIdentifier);
            Institution institution = institutionRepository.findById(institutionIdentifier).orElseThrow(NotFoundException::new);
            if (!user.hasAgreedWithAup(institution)) {
                user.addAup(new Aup(institution));
            }
        });
        userRepository.save(user);
        return createdResponse();
    }

}
