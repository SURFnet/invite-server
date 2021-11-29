package guests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import guests.config.HashGenerator;
import guests.domain.*;
import guests.repository.*;
import io.restassured.RestAssured;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.Period;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "oidc.introspection_uri=http://localhost:8081/introspect"
        })
@SuppressWarnings("unchecked")
public abstract class AbstractTest {

    protected final String HASH = HashGenerator.generateHash();

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected InstitutionRepository institutionRepository;

    @Autowired
    protected ApplicationRepository applicationRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected InvitationRepository invitationRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @RegisterExtension
    WireMockExtension mockServer = new WireMockExtension(8081);

    @LocalServerPort
    protected int port;

    @BeforeEach
    void beforeEach() {
        institutionRepository.deleteAll();

        seed();
        RestAssured.port = port;
    }

    private void seed() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("Principal", "N/A", "ADMIN"));
        List<Institution> institutions = Arrays.asList(
                new Institution("https://ut", "utrecht.nl", "https://utrecht.nl/aup", "V1"),
                new Institution("https://uva", "uva.nl", "https://uva.nl/aup", "V1")
        );
        institutionRepository.saveAll(institutions);

        List<Application> applications = Arrays.asList(
                new Application(institutions.get(0), "CANVAS"),
                new Application(institutions.get(1), "blackboard")
        );
        applicationRepository.saveAll(applications);

        Role role = new Role("administrator", applications.get(0));
        role = roleRepository.save(role);

        User mary = new User(Authority.INSTITUTION_ADMINISTRATOR, "mdoe@surf.nl", "Mary", "Doe", "mdoe@surf.nl", institutions.get(0));
        mary.getRoles().add(new UserRole(mary, role, Instant.now().plus(Period.ofDays(90))));
        User guest = new User(Authority.GUEST, "guest@ut.nl", "fn", "ln", "guest@ut.nl", institutions.get(0));
        List<User> users = Arrays.asList(mary, guest);
        userRepository.saveAll(users);

        Invitation invitation = new Invitation(Authority.INVITER, Status.OPEN, HASH, mary);
        invitation.addInvitationRole(new InvitationRole(role));
        List<Invitation> invitations = Arrays.asList(invitation);
        invitationRepository.saveAll(invitations);
    }

    protected String opaqueAccessToken(String eppn, String responseJsonFileName, String... scopes) throws IOException {
        List<String> scopeList = new ArrayList<>(Arrays.asList(scopes));
        scopeList.add("openid");

        String introspectResult = IOUtils.toString(new ClassPathResource(responseJsonFileName).getInputStream(), Charset.defaultCharset().name());
        String introspectResultWithScope = String.format(introspectResult, eppn, String.join(" ", scopeList));
        stubFor(post(urlPathMatching("/introspect")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(introspectResultWithScope)));
        return UUID.randomUUID().toString();
    }

    protected Map<String, Object> convertObjectToMap(Object o) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(o);
        return objectMapper.readValue(json, Map.class);
    }

}
