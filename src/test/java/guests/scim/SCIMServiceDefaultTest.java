package guests.scim;

import com.fasterxml.jackson.core.JsonProcessingException;
import guests.AbstractMailTest;
import guests.domain.*;
import lombok.SneakyThrows;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SCIMServiceDefaultTest extends AbstractMailTest {

    @Autowired
    private SCIMServiceDefault scimService;

    @Test
    void newUserRequest() throws JsonProcessingException {
        String serviceProviderId = stubForCreateUser();
        User user = seedUser();
        scimService.newUserRequest(user);

        assertEquals(serviceProviderId, user.getUserRoles().iterator().next().getServiceProviderId());
        assertNoSCIMFailures();
    }

    @Test
    void newUserRequestEmail() throws Exception {
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
        seedSCIMUserRole(user);

        scimService.updateUserRequest(user);
        assertNoSCIMFailures();
    }

    @Test
    void updateUnknownUserRequest() throws JsonProcessingException {
        User user = seedUser();
        stubForCreateUser();

        scimService.updateUserRequest(user);
        assertNoSCIMFailures();
        assertNotNull(user.getUserRoles().iterator().next().getServiceProviderId());
    }

    @Test
    void updateUserRequestMail() throws Exception {
        User user = seedUserWithEmailProvisioning();
        String serviceProviderId = seedSCIMUserRole(user);

        scimService.updateUserRequest(user);

        MimeMessageParser parser = mailMessage();
        String htmlContent = parser.getHtmlContent();

        assertTrue(htmlContent.contains(serviceProviderId));
        assertNoSCIMFailures();
    }

    @Test
    void deleteUserRequest() throws JsonProcessingException {
        User user = seedUser();
        String serviceProviderId = seedSCIMUserRole(user);
        user.getUserRoles().iterator().next().getRole().setServiceProviderId(serviceProviderId);

        stubForUpdateRole();
        stubForDeleteUser();

        scimService.deleteUserRequest(user);
        assertNoSCIMFailures();
    }

    @Test
    void deleteUnknownUserRequest() throws JsonProcessingException {
        User user = seedUser();
        stubForCreateRole();

        scimService.deleteUserRequest(user);
        assertNoSCIMFailures();
    }

    @Test
    void deleteUserRequestMail() throws Exception {
        User user = seedUserWithEmailProvisioning();
        String serviceProviderId = seedSCIMUserRole(user);

        scimService.deleteUserRequest(user);
        assertNoSCIMFailures();

        List<MimeMessageParser> mimeMessageParsers = allMailMessages(2);
        mimeMessageParsers.forEach(parser -> {
            String htmlContent = parser.getHtmlContent();
            if (htmlContent.contains("urn:ietf:params:scim:schemas:core:2.0:Group")) {
                assertTrue(htmlContent.contains("members&quot; : [ ]"));
            } else {
                assertTrue(htmlContent.contains(serviceProviderId));
            }
        });
    }

    @Test
    void newRoleRequestNoProvisioning() {
        scimService.newRoleRequest(new Role("name", new Application()));
        assertNoSCIMFailures();
    }

    @Test
    void newRoleRequestEmailProvisioning() throws Exception {
        User user = this.seedUserWithEmailProvisioning();
        Application application = user.getUserRoles().iterator().next().getRole().getApplication();
        scimService.newRoleRequest(new Role("xyz", application));

        MimeMessageParser parser = mailMessage();
        String htmlContent = parser.getHtmlContent();

        assertTrue(htmlContent.contains("xyz"));
        assertNoSCIMFailures();
    }

    @Test
    void updateRoleRequestWithNoServiceProviderId() throws JsonProcessingException {
        User user = seedUser();
        UserRole userRole = user.getUserRoles().iterator().next();
        stubForCreateRole();
        scimService.updateRoleRequest(userRole, OperationType.Add);
        assertNoSCIMFailures();
    }

    @Test
    void updateRoleRequest() throws JsonProcessingException {
        User user = seedUser();
        seedSCIMUserRole(user);
        UserRole userRole = user.getUserRoles().iterator().next();
        Role role = userRole.getRole();
        role.setServiceProviderId(UUID.randomUUID().toString());
        roleRepository.save(role);

        stubForUpdateRole();
        scimService.updateRoleRequest(userRole, OperationType.Add);
        assertNoSCIMFailures();
    }

    @Test
    void updateRoleRequestUnprovisionedUsers() throws JsonProcessingException {
        User user = seedUser();

        UserRole userRole = user.getUserRoles().iterator().next();
        Role role = userRole.getRole();
        role.setServiceProviderId(UUID.randomUUID().toString());
        roleRepository.save(role);

        stubForCreateUser();
        stubForUpdateRole();

        scimService.updateRoleRequest(userRole, OperationType.Add);
        assertNoSCIMFailures();
    }

    @Test
    void updateRoleRequestNoProvisioning() {
        User user = seedUser();

        UserRole userRole = user.getUserRoles().iterator().next();
        userRole.getRole().setApplication(new Application());
        scimService.updateRoleRequest(userRole, OperationType.Remove);
        assertNoSCIMFailures();
    }

    @Test
    void deleteRolesRequestNoProvisioning() {
        scimService.deleteRolesRequest(new Role("name", new Application()));
        assertNoSCIMFailures();
    }

    @Test
    void deleteRolesRequestNoProvisioningNoServiceId() {
        Application application = new Application();
        application.setProvisioningHookUrl("https://prov");
        scimService.deleteRolesRequest(new Role("name", application));
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
        seedSCIMUserRole(user);

        scimService.deleteUserRequest(user);

        List<SCIMFailure> failures = scimFailureRepository.findAll();
        assertEquals(2, failures.size());
    }

    @Test
    void updateUserRequestFailure() {
        User user = seedUser();
        UserRole userRole = user.getUserRoles().iterator().next();
        String serviceProviderId = UUID.randomUUID().toString();
        userRole.setServiceProviderId(serviceProviderId);
        userRoleRepository.save(userRole);
        user.getUserRoles().iterator().next().getRole().setServiceProviderId(serviceProviderId);

        scimService.updateUserRequest(user);
        assertSCIMFailure("http://localhost:8081/scim/v1/users/" + serviceProviderId);
    }

    @Test
    void createRoleRequestFailure() {
        User user = seedUser();
        Role role = user.getUserRoles().iterator().next().getRole();

        scimService.newRoleRequest(role);
        assertSCIMFailure("http://localhost:8081/scim/v1/groups");
    }

    @Test
    void updateRoleRequestFailure() throws Exception {
        User user = seedUser();
        String serviceProviderId = seedSCIMUserRole(user);

        UserRole userRole = user.getUserRoles().iterator().next();
        Role role = userRole.getRole();
        role.setServiceProviderId(serviceProviderId);
        role.setId(1L);

        scimService.updateRoleRequest(userRole, OperationType.Add);
        assertSCIMFailure("http://localhost:8081/scim/v1/groups/" + serviceProviderId);
    }

    @Test
    void deleteRoleRequestFailure() throws Exception {
        User user = seedUser();
        String serviceProviderId = UUID.randomUUID().toString();
        Role role = user.getUserRoles().iterator().next().getRole();
        role.setId(1L);
        role.setServiceProviderId(serviceProviderId);

        scimService.deleteRolesRequest(role);
        assertSCIMFailure("http://localhost:8081/scim/v1/groups/" + serviceProviderId);
    }

    private void assertNoSCIMFailures() {
        assertEquals(0, scimFailureRepository.count());
    }

    @SneakyThrows
    private void assertSCIMFailure(String uri) {
        List<SCIMFailure> failures = scimFailureRepository.findAll();
        assertEquals(1, failures.size());

        SCIMFailure scimFailure = failures.get(0);
        assertEquals(uri, scimFailure.getUri());
        assertNotNull(scimFailure.getCreatedAt());

        MimeMessageParser parser = mailMessage();
        String htmlContent = parser.getHtmlContent();

        assertTrue(htmlContent.contains("SCIMFailure"));
    }

    private User seedUserWithEmailProvisioning() {
        User user = seedUser();
        Application application = user.getUserRoles().iterator().next().getRole().getApplication();
        application.setProvisioningHookUrl(null);
        application.setProvisioningHookEmail("admin@uva.nl");

        applicationRepository.save(application);

        return user;
    }

    private String seedSCIMUserRole(User user) {
        UserRole userRole = user.getUserRoles().iterator().next();
        String serviceProviderId = stubForUpdateUser();
        userRole.setServiceProviderId(serviceProviderId);
        userRoleRepository.save(userRole);
        return serviceProviderId;
    }
}