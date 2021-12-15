package guests.api;

import guests.domain.Institution;
import guests.domain.ObjectExists;
import guests.domain.Validation;
import guests.exception.NotFoundException;
import guests.repository.InstitutionRepository;
import guests.validation.EmailFormatValidator;
import guests.validation.FormatValidator;
import guests.validation.URLFormatValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static guests.api.Shared.doesExists;
import static java.util.stream.Collectors.toMap;

@RestController
@RequestMapping(value = "/guests/api/validations", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class ValidationController {

    private final Map<String, FormatValidator> validators;

    public ValidationController() {
        this.validators = Stream.of(
                        new EmailFormatValidator(),
                        new URLFormatValidator())
                .collect(toMap(FormatValidator::formatName, Function.identity()));
    }

    @PostMapping("validate")
    public ResponseEntity<Map<String, Boolean>> validate(@RequestBody Validation validation) {
        boolean valid = this.validators.computeIfAbsent(validation.getType(), key -> {
            throw new IllegalArgumentException(String.format("No validation defined for %s", key));
        }).isValid(validation.getValue());
        return ResponseEntity.ok(Collections.singletonMap("valid", valid));
    }


}
