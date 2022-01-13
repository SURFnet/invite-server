package guests.api;

import guests.AbstractMailTest;
import guests.domain.Status;
import io.restassured.http.ContentType;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationMailControllerTest extends AbstractMailTest {

    @Test
    void resend() throws Exception {
        Long id = invitationRepository.findByHashAndStatus(INVITATION_HASH, Status.OPEN).get().getId();
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
                .put("/guests/api/invitations/resend")
                .then()
                .statusCode(201);

        MimeMessageParser parser = mailMessage();

        String htmlContent = parser.getHtmlContent();
        assertTrue(htmlContent.contains("Please"));
    }

}