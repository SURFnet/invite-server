package guests.scim;

import com.fasterxml.jackson.core.JsonProcessingException;
import guests.AbstractMailTest;
import guests.domain.*;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void newRoleRequestNoProvisioning() {
        scimService.newRoleRequest(new Role("name", new Application()));
    }

    @Test
    void updateRoleRequestNoProvisioning() {
        scimService.updateRoleRequest(new Role("name", new Application()), Collections.emptyList());
    }

    @Test
    void deleteRolesRequestNoProvisioning() {
        scimService.deleteRolesRequest(new Role("name", new Application()));
    }

    @Test
    void newUserRequestFailure() {
        User user = seedUser();

        scimService.newUserRequest(user);
        assertSCIMFailure("http://localhost:8081/scim/v1/users");
    }

    @Test
    void deleteUserRequestFailure() {
        User user = seedUser();
        String serviceProviderId = UUID.randomUUID().toString();
        UserRole userRole = user.getRoles().iterator().next();
        userRole.setServiceProviderId(serviceProviderId);

        scimService.deleteUserRequest(user);
        assertSCIMFailure("http://localhost:8081/scim/v1/users/" + serviceProviderId);
    }

    @Test
    void updateUserRequestFailure() {
        User user = seedUser();
        String serviceProviderId = UUID.randomUUID().toString();
        UserRole userRole = user.getRoles().iterator().next();
        userRole.setServiceProviderId(serviceProviderId);

        scimService.updateUserRequest(user);
        assertSCIMFailure("http://localhost:8081/scim/v1/users/" + serviceProviderId);
    }

    @Test
    void createRoleRequestFailure() {
        User user = seedUser();
        Role role = user.getRoles().iterator().next().getRole();

        scimService.newRoleRequest(role);
        assertSCIMFailure("http://localhost:8081/scim/v1/groups");
    }

    @Test
    void updateRoleRequestFailure() {
        User user = seedUser();
        String serviceProviderId = UUID.randomUUID().toString();
        UserRole userRole = user.getRoles().iterator().next();
        userRole.setServiceProviderId(serviceProviderId);

        Role role = userRole.getRole();
        role.setServiceProviderId(serviceProviderId);
        role.setId(1L);

        scimService.updateRoleRequest(role, Collections.singletonList(user));
        assertSCIMFailure("http://localhost:8081/scim/v1/groups/" + serviceProviderId);
    }

    @Test
    void deleteRoleRequestFailure() {
        User user = seedUser();
        String serviceProviderId = UUID.randomUUID().toString();
        Role role = user.getRoles().iterator().next().getRole();
        role.setId(1L);
        role.setServiceProviderId(serviceProviderId);

        scimService.deleteRolesRequest(role);
        assertSCIMFailure("http://localhost:8081/scim/v1/groups/" + serviceProviderId);
    }

    private void assertSCIMFailure(String uri) {
        List<SCIMFailure> failures = scimFailureRepository.findAll();
        assertEquals(1, failures.size());

        SCIMFailure scimFailure = failures.get(0);
        assertEquals(uri, scimFailure.getUri());
        assertNotNull(scimFailure.getCreatedAt());
    }

    private User seedUser() {
        User user = user();
        Institution institution = getInstitution(user);
        institutionRepository.save(institution);

        Application application = this.application(institution, "https://entity");
        String provisioningUri = "http://localhost:8081";
        application.setProvisioningHookUrl(provisioningUri);
        application.setProvisioningHookUsername("user");
        application = applicationRepository.save(application);

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