package guests.api;

import guests.AbstractMailTest;
import guests.config.HashGenerator;
import guests.domain.*;
import io.restassured.http.ContentType;
import lombok.SneakyThrows;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class SCIMScenarioTest extends AbstractMailTest {

    @Test
    void scimFlow() throws Exception {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://utrecht").get();
        Application application = new Application();
        application.setName("LMS");
        application.setEntityId("https://lms");
        application.setLandingPage("https://lms");
        application.setInstitution(institution);
        application.setProvisioningHookEmail("hoo@scim.flow");
        applicationRepository.save(application);

        List<Role> roles = Arrays.asList(
                new Role("Guests", application),
                new Role("Students", application)
        );
        roles.forEach(role -> role.setServiceProviderId(UUID.randomUUID().toString()));
        roleRepository.saveAll(roles);

        String invitationHash = HashGenerator.generateHash();
        User inviter = userRepository.findByEduPersonPrincipalNameIgnoreCase("inviter@utrecht.nl").get();
        Invitation invitation = new Invitation(Authority.GUEST, Status.OPEN, invitationHash, inviter, institution, "hoo@scim.flow");
        roles.forEach(role -> invitation.addInvitationRole(new InvitationRole(role)));
        invitationRepository.save(invitation);

        Map<String, Object> invitationAccept = new HashMap<>();
        invitationAccept.put("hash", invitationHash);
        invitationAccept.put("status", Status.ACCEPTED);

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("new@utrecht.nl", "introspect.json"))
                .body(invitationAccept)
                .post("/api/v1/invitations")
                .then()
                .statusCode(201);

        User other = userRepository.findByEduPersonPrincipalNameIgnoreCase("new@utrecht.nl").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("id", other.getId())
                .delete("/api/v1/users/{id}")
                .then()
                .statusCode(201);

        List<MimeMessageParser> mimeMessageParsers = allMailMessages(6);
        List<String> subjects = mimeMessageParsers.stream().map(this::getSubject).sorted().collect(Collectors.toList());
        assertIterableEquals(Arrays.asList(
                "SCIM groups: UPDATE",
                "SCIM groups: UPDATE",
                "SCIM groups: UPDATE",
                "SCIM groups: UPDATE",
                "SCIM users: CREATE",
                "SCIM users: DELETE"
        ), subjects);
    }

}