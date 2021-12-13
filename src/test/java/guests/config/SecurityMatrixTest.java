package guests.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import guests.domain.Authority;
import guests.domain.User;
import guests.security.SecurityMatrix;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.*;

class SecurityMatrixTest {

    private final SecurityMatrix securityMatrix = new SecurityMatrix(
            new ObjectMapper().readValue(
                    IOUtils.toString(new ClassPathResource("securityMatrix.json").getInputStream(), Charset.defaultCharset()),
                    new TypeReference<>() {
                    }));

    public SecurityMatrixTest() throws IOException {
    }

    @Test
    void isAllowed() {
        doIsAllowed(Authority.SUPER_ADMIN, "/guests/api/institutions", "post", true);
        doIsAllowed(Authority.INSTITUTION_ADMINISTRATOR, "/guests/api/institutions", "post", false);
        doIsAllowed(Authority.INSTITUTION_ADMINISTRATOR, "/guests/api/applications/13", "get", true);
        doIsAllowed(Authority.INVITER, "/guests/api/applications/13", "get", true);
        doIsAllowed(Authority.INVITER, "/guests/api/applications", "get", true);
        doIsAllowed(Authority.GUEST, "/nope", "get", false);
    }

    private void doIsAllowed(Authority authority, String requestURI, String httpMethod, boolean expected) {
        User user = new User();
        user.setAuthority(authority);
        boolean allowed = securityMatrix.isAllowed(requestURI, httpMethod, user);
        assertEquals(expected, allowed);
    }
}