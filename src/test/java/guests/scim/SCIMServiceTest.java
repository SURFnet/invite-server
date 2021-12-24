package guests.scim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import guests.AbstractMailTest;
import guests.AbstractTest;
import guests.domain.Application;
import guests.domain.Role;
import guests.domain.User;
import guests.domain.UserRole;
import lombok.SneakyThrows;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import javax.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.Period;
import java.util.Collections;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SCIMServiceTest extends AbstractMailTest {

    @Autowired
    private SCIMService scimService;

    @Test
    void newUserRequest() throws JsonProcessingException {
        String serviceProviderId = UUID.randomUUID().toString();
        stubFor(post(urlPathMatching("/scim/v1/users")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(Collections.singletonMap("id", serviceProviderId)))));
        User user = seedUser();
        scimService.newUserRequest(user);

        assertEquals(serviceProviderId, user.getRoles().iterator().next().getServiceProviderId());
    }

    @Test
    void newUserRequestEmail() {
        User user = seedUserWithEmailProvisioning();
        scimService.newUserRequest(user);

        MimeMessageParser parser = mailMessage();
        String htmlContent = parser.getHtmlContent();

        assertTrue(htmlContent.contains(user.getEmail()));
    }

    @Test
    void updateUserRequest() throws JsonProcessingException {
        User user = seedUser();
        String serviceProviderId = UUID.randomUUID().toString();
        UserRole userRole = user.getRoles().iterator().next();
        userRole.setServiceProviderId(serviceProviderId);
        stubFor(patch(urlPathMatching(String.format("/scim/v1/users/%s", serviceProviderId)))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Collections.singletonMap("id", serviceProviderId)))));

        scimService.updateUserRequest(user);
    }

    @Test
    void updateUserRequestMail() {
        User user = seedUserWithEmailProvisioning();
        String serviceProviderId = UUID.randomUUID().toString();
        UserRole userRole = user.getRoles().iterator().next();
        userRole.setServiceProviderId(serviceProviderId);

        scimService.updateUserRequest(user);

        MimeMessageParser parser = mailMessage();
        String htmlContent = parser.getHtmlContent();

        assertTrue(htmlContent.contains(serviceProviderId));
    }

    @Test
    void deleteUserRequest() {
        User user = seedUser();
        String serviceProviderId = UUID.randomUUID().toString();
        UserRole userRole = user.getRoles().iterator().next();
        userRole.setServiceProviderId(serviceProviderId);

        stubForDeleteUser();

        scimService.deleteUserRequest(user);
    }

    @Test
    void deleteUserRequestMail() {
        User user = seedUserWithEmailProvisioning();
        String serviceProviderId = UUID.randomUUID().toString();
        UserRole userRole = user.getRoles().iterator().next();
        userRole.setServiceProviderId(serviceProviderId);

        scimService.deleteUserRequest(user);

        MimeMessageParser parser = mailMessage();
        String htmlContent = parser.getHtmlContent();

        assertTrue(htmlContent.contains(serviceProviderId));
    }

    private User seedUser() {
        User user = user();
        Application application = this.application(user.getInstitution(), "https://entity");
        String provisioningUri = "http://localhost:8081";
        application.setProvisioningHookUrl(provisioningUri);
        application.setProvisioningHookUsername("user");
        application.setId(1L);
        Role role = new Role("administrator", application);
        user.addUserRole(new UserRole(role, Instant.now().plus(Period.ofDays(365))));
        return user;
    }

    private User seedUserWithEmailProvisioning() {
        User user = seedUser();
        Application application = user.getRoles().iterator().next().getRole().getApplication();
        application.setProvisioningHookUrl(null);
        application.setProvisioningHookEmail("admin@uva.nl");
        return user;
    }
}