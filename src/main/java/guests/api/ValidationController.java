package guests.api;

import guests.domain.Validation;
import guests.validation.EmailFormatValidator;
import guests.validation.FormatValidator;
import guests.validation.URLFormatValidator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@RestController
@RequestMapping(value = "/api/v1/validations", produces = MediaType.APPLICATION_JSON_VALUE)
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
