package guests.api;

import guests.AbstractMailTest;
import guests.AbstractTest;
import guests.domain.*;
import io.restassured.http.ContentType;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class InvitationMailControllerTest extends AbstractMailTest {

    @Test
    void resend() {
        Long id = invitationRepository.findByHashAndStatus(INVITATION_HASH, Status.OPEN).get().getId();
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("id", id);
        invitation.put("message", "Please...");

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .body(invitation)
                .put("/guests/api/invitations/resend")
                .then()
                .statusCode(201);

        MimeMessageParser parser = mailMessage();

        String htmlContent = parser.getHtmlContent();
        assertTrue(htmlContent.contains("Please"));
    }

}