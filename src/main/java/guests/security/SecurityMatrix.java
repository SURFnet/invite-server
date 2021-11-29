package guests.security;

import guests.domain.Authority;
import guests.domain.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SecurityMatrix {

    private static final Log LOG = LogFactory.getLog(SecurityMatrix.class);

    private final Pattern pattern = Pattern.compile("/guests/api/(?<api>[^/]+).*");
    private final Map<String, Map<String, Authority>> matrix;

    public SecurityMatrix(Map<String, Map<String, String>> matrix) {
        this.matrix = matrix.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toUpperCase(),
                        entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
                                e -> e.getKey().toUpperCase(),
                                e -> Authority.valueOf(e.getValue().toUpperCase()))
                        )));
    }

    public boolean isAllowed(String requestURI, String httpMethod, User user) {
        if (user.getAuthority().equals(Authority.SUPER_ADMIN)) {
            return true;
        }
        Matcher matcher = pattern.matcher(requestURI);
        boolean found = matcher.find();
        if (!found) {
            LOG.error(String.format("Invalid requestURI for security checking: '%s'", requestURI));
            return false;
        }
        String api = matcher.group("api").toUpperCase();
        Authority requiredAuthority = matrix.getOrDefault(api, Collections.emptyMap())
                .getOrDefault(httpMethod.toUpperCase(), Authority.SUPER_ADMIN);
        return user.getAuthority().isAllowed(requiredAuthority);
    }

}
