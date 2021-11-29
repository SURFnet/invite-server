package guests.api;

import guests.AbstractTest;
import guests.domain.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InvitationControllerTest extends AbstractTest {

    @Test
    void get() throws Exception {
        Invitation invitation = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("unknown@user.nl", "introspect.json"))
                .pathParam("hash", HASH)
                .get("/guests/api/invitations/{hash}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", Invitation.class);
        assertEquals(Status.OPEN, invitation.getStatus());
        assertEquals("administrator", invitation.getRoles().iterator().next().getRole().getName());
    }

    @Test
    void post() throws Exception {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", HASH);
        invitation.put("status", Status.ACCEPTED);
        User user = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("new@user.nl", "introspect.json"))
                .body(invitation)
                .post("/guests/api/invitations")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals("new@user.nl", user.getEduPersonPrincipalName());
    }

    @Test
    void put() throws Exception {
        Role role = roleRepository.findAll().get(0);
        Invitation invitation = new Invitation(Authority.GUEST,
                "Please accept",
                "guest@example.com",
                true,
                Collections.singleton(new InvitationRole(role)));
        Invitation newInvitation = given()
                        .when()
                        .accept(ContentType.JSON)
                        .contentType(ContentType.JSON)
                        .auth().oauth2(opaqueAccessToken("mdoe@surf.nl", "introspect.json"))
                        .body(invitation)
                        .put("/guests/api/invitations")
                        .then()
                        .extract()
                        .body()
                        .jsonPath()
                        .getObject(".", Invitation.class);
        assertEquals(Status.OPEN, newInvitation.getStatus());
        assertNull(newInvitation.getHash());
    }
}