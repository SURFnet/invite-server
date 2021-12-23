package guests.scim;

import com.fasterxml.jackson.core.JsonProcessingException;
import guests.AbstractTest;
import guests.domain.Application;
import guests.domain.Role;
import guests.domain.User;
import guests.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.Period;
import java.util.Collections;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SCIMServiceTest extends AbstractTest {

    @Autowired
    private SCIMService scimService;
    private String provisioningUri = "http://localhost:8081/scim";

    @Test
    void newUserRequest() throws JsonProcessingException {
        String serviceProviderId = UUID.randomUUID().toString();
        stubFor(post(urlPathMatching("/scim/v1/Users")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(Collections.singletonMap("id", serviceProviderId)))));
        User user = seedUser();
        scimService.newUserRequest(user);

        assertEquals(serviceProviderId, user.getRoles().iterator().next().getServiceProviderId());
    }

    private User seedUser() {
        User user = user();
        Application application = new Application(user.getInstitution(), "https://entity", "secret");
        application.setProvisioningHookUrl(provisioningUri);
        application.setProvisioningHookUsername("user");
        application.setId(1L);
        Role role = new Role("administrator", application);
        user.addUserRole(new UserRole(role, Instant.now().plus(Period.ofDays(365))));
        return user;
    }

    @Test
    void updateUserRequest() {
    }

    @Test
    void deleteUserRequest() {
    }
}