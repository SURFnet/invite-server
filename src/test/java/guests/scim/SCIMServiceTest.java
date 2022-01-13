package guests.scim;

import guests.AbstractMailTest;
import guests.domain.*;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SCIMServiceTest extends AbstractMailTest {

    @Autowired
    private SCIMService scimService;

    @Test
    void newUserRequest() {
        String serviceProviderId = stubForCreateUser();
        User user = seedUser();
        scimService.newUserRequest(user);

        assertEquals(serviceProviderId, user.getRoles().iterator().next().getServiceProviderId());
        assertNoSCIMFailures();
    }

    @Test
    void newUserRequestEmail() {
        User user = seedUserWithEmailProvisioning();
        scimService.newUserRequest(user);

        MimeMessageParser parser = mailMessage();
        String htmlContent = parser.getHtmlContent();

        assertTrue(htmlContent.contains(user.getEmail()));
        assertNoSCIMFailures();
    }

    @Test
    void updateUserRequest() {
        User user = seedUser();
        String serviceProviderId = stubForUpdateUser();
        UserRole userRole = user.getRoles().iterator().next();
        userRole.setServiceProviderId(serviceProviderId);

        scimService.updateUserRequest(user);
        assertNoSCIMFailures();
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
        assertNoSCIMFailures();
    }

    @Test
    void deleteUserRequest() {
        User user = seedUser();
        String serviceProviderId = UUID.randomUUID().toString();
        UserRole userRole = user.getRoles().iterator().next();
        userRole.setServiceProviderId(serviceProviderId);

        stubForDeleteUser();

        scimService.deleteUserRequest(user);
        assertNoSCIMFailures();
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
        assertNoSCIMFailures();
    }

    @Test
    void newRoleRequestNoProvisioning() {
        scimService.newRoleRequest(new Role("name", new Application()));
        assertNoSCIMFailures();
    }

    @Test
    void newRoleRequestEmailProvisioning() {
        User user = this.seedUserWithEmailProvisioning();
        Application application = user.getRoles().iterator().next().getRole().getApplication();
        scimService.newRoleRequest(new Role("xyz", application));

        MimeMessageParser parser = mailMessage();
        String htmlContent = parser.getHtmlContent();

        assertTrue(htmlContent.contains("xyz"));
        assertNoSCIMFailures();
    }

    @Test
    void updateRoleRequestWithNoServiceProviderId() {
        User user = seedUser();
        UserRole userRole = user.getRoles().iterator().next();
        Role role = userRole.getRole();
        role.setId(1L);

        scimService.updateRoleRequest(role, Collections.singletonList(user));
        assertNoSCIMFailures();

    }

    @Test
    void updateRoleRequest() {
        User user = seedUser();
        UserRole userRole = user.getRoles().iterator().next();
        String serviceProviderId = stubForUpdateRole();
        userRole.setServiceProviderId(serviceProviderId);
        Role role = userRole.getRole();
        role.setId(1L);

        scimService.updateRoleRequest(role, Collections.singletonList(user));
        assertNoSCIMFailures();

    }

    @Test
    void updateRoleRequestNoProvisioning() {
        scimService.updateRoleRequest(new Role("name", new Application()), Collections.emptyList());
        assertNoSCIMFailures();
    }

    @Test
    void deleteRolesRequestNoProvisioning() {
        scimService.deleteRolesRequest(new Role("name", new Application()));
        assertNoSCIMFailures();
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

        MimeMessageParser parser = mailMessage();
        String htmlContent = parser.getHtmlContent();

        assertTrue(htmlContent.contains(serviceProviderId));
    }

    private void assertNoSCIMFailures() {
        assertEquals(0, scimFailureRepository.count());
    }

    private void assertSCIMFailure(String uri) {
        List<SCIMFailure> failures = scimFailureRepository.findAll();
        assertEquals(1, failures.size());

        SCIMFailure scimFailure = failures.get(0);
        assertEquals(uri, scimFailure.getUri());
        assertNotNull(scimFailure.getCreatedAt());
    }

    private User seedUserWithEmailProvisioning() {
        User user = seedUser();
        Application application = user.getRoles().iterator().next().getRole().getApplication();
        application.setProvisioningHookUrl(null);
        application.setProvisioningHookEmail("admin@uva.nl");
        return user;
    }
}