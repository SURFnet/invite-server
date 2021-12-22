package guests.api;

import guests.AbstractTest;
import guests.domain.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.*;

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
    @SuppressWarnings("unchecked")
    void allByInstitution() throws Exception {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://ut").get();
        List<Invitation> invitations = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/guests/api/invitations/institution/{institutionId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Invitation.class);
        assertEquals(1, invitations.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void allByApplication() throws Exception {
        Application application = applicationRepository.findByEntityIdIgnoreCase("CANVAS").get();
        List<Invitation> invitations = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("applicationId", application.getId())
                .get("/guests/api/invitations/application/{applicationId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Invitation.class);
        assertEquals(1, invitations.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void allByInstitutionNotAllowed() throws Exception {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://uva").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("mdoe@surf.nl", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/guests/api/invitations/institution/{institutionId}")
                .then()
                .statusCode(403);
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
        InvitationRequest invitationRequest = new InvitationRequest(invitation, Arrays.asList("guest@example.com", "admin@example.com"));

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("mdoe@surf.nl", "introspect.json"))
                .body(invitationRequest)
                .put("/guests/api/invitations")
                .then()
                .statusCode(201);
        assertEquals(3, invitationRepository.count());
    }
}