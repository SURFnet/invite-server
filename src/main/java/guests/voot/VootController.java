package guests.voot;

import guests.domain.Application;
import guests.domain.Role;
import guests.domain.UserRole;
import guests.domain.Validation;
import guests.repository.UserRepository;
import guests.validation.EmailFormatValidator;
import guests.validation.FormatValidator;
import guests.validation.URLFormatValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@RestController
@RequestMapping(value = "/api/voot", produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
public class VootController {

    private final UserRepository userRepository;
    private final String groupUrnPrefix;

    public VootController(UserRepository userRepository, @Value("${voot.group_urn_domain}") String groupUrnDomain) {
        this.userRepository = userRepository;
        this.groupUrnPrefix = String.format("urn:collab:group:%s", groupUrnDomain);
    }

    @GetMapping("/{unspecified_id}")
    public ResponseEntity<List<Map<String, String>>> getGroupMemberships(@PathVariable("unspecified_id") String unspecifiedId) {
        List<Map<String, String>> res = userRepository.findByUnspecifiedIdIgnoreCase(unspecifiedId)
                .map(user -> user.getRoles().stream().map(this::parseUserRole).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        return ResponseEntity.ok(res);
    }

    private Map<String, String> parseUserRole(UserRole userRole) {
        Map<String, String> res = new HashMap<>();
        Role role = userRole.getRole();
        Application application = role.getApplication();
        String urn = String.format("%s:%s:%s:%s",
                groupUrnPrefix,
                application.getInstitution().getHomeInstitution().toLowerCase(),
                application.getDisplayName().toLowerCase(),
                role.getName()).toLowerCase();
        res.put("urn", urn);
        res.put("name", role.getName());
        return res;
    }

}
