package guests.api;

import guests.AbstractMailTest;
import guests.domain.*;
import io.restassured.http.ContentType;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationMailControllerTest extends AbstractMailTest {

    @Test
    void resend() throws Exception {
        Long id = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get().getId();
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("id", id);
        invitation.put("message", "Please...");
        invitation.put("expiryDate", Instant.now());

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(invitation)
                .put("/api/v1/invitations/resend")
                .then()
                .statusCode(201);

        MimeMessageParser parser = mailMessage();

        String htmlContent = parser.getHtmlContent();
        assertTrue(htmlContent.contains("Please"));
    }

    @Test
    void postUnprovisionedRoleCombination() throws Exception {
        /*
         * Set up test data: two roles, one SCIM provisioned and the other not and
         * accept with existing User that already has a role in the application for the
         * role without serviceProviderID
         *
         * Expectation: the Role is SCIM provisioned with one member and the other role
         * is patched with the new member
         */
        Role administratorCanvas = roleRepository.findByName("administratorCanvas").get();
        administratorCanvas.setServiceProviderId(null);
        roleRepository.save(administratorCanvas);

        Invitation invitation = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get();
        Role guestBlackboard = roleRepository.findByName("guestBlackboard").get();
        invitation.addInvitationRole(new InvitationRole(guestBlackboard, null));
        invitationRepository.save(invitation);

        Map<String, Object> invitationMap = new HashMap<>();
        invitationMap.put("hash", INVITATION_UTRECHT_HASH);
        invitationMap.put("status", Status.ACCEPTED);

        List<Application> applications = applicationRepository.findAll();
        applications.forEach(application -> {
            application.setProvisioningHookUrl(null);
            application.setProvisioningHookEmail("test@test.org");
        });
        applicationRepository.saveAll(applications);

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("guest@utrecht.nl", "introspect-guest.json"))
                .body(invitationMap)
                .post("/api/v1/invitations")
                .then()
                .statusCode(201);

        List<MimeMessageParser> parsers = allMailMessages(3);
        Map<String, String> messages = parsers.stream()
                .collect(Collectors.toMap(parser -> getSubject(parser), parser -> getPlainContent(parser)));

        assertEquals(3, messages.size());
        assertTrue(messages.containsKey("SCIM users: CREATE"));
        assertTrue(messages.get("SCIM groups: UPDATE")
                .contains("urn:collab:group:test.eduid.nl:uva.nl:blackboard:guestblackboard"));
        assertTrue(messages.get("SCIM groups: CREATE")
                .contains("urn:collab:group:test.eduid.nl:utrecht.nl:canvas:administratorcanvas"));
    }


}