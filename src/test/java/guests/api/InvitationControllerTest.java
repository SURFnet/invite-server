package guests.api;

import guests.AbstractTest;
import guests.domain.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InvitationControllerTest extends AbstractTest {

    @Test
    void get() {
        Invitation invitation = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("unknown@user.nl", "introspect.json"))
                .pathParam("hash", INVITATION_HASH)
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
    void getExistingUser() {
        Map<String, Object> invitation = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("hash", INVITATION_EMAIL_EQUALITY_HASH)
                .get("/guests/api/invitations/{hash}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getMap(".");
        assertEquals(true, invitation.get("emailEqualityConflict"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void allByInstitution() throws Exception {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://utrecht").get();
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
        assertEquals(2, invitations.size());
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
        assertEquals(2, invitations.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void allByInstitutionNotAllowed() {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://uva").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/guests/api/invitations/institution/{institutionId}")
                .then()
                .statusCode(403);
    }

    @Test
    void post() {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", INVITATION_HASH);
        invitation.put("status", Status.ACCEPTED);

        this.stubForCreateUser();
        this.stubForUpdateGroup();

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
    void postEmailInequality() {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", INVITATION_EMAIL_EQUALITY_HASH);
        invitation.put("status", Status.ACCEPTED);

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("new@user.nl", "introspect.json"))
                .body(invitation)
                .post("/guests/api/invitations")
                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void postExistingUser() {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", INVITATION_HASH);
        invitation.put("status", Status.ACCEPTED);
        //prevent user_roles_unique_user_role exception
        User userFromDB = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        userFromDB.setRoles(new HashSet<>());
        userRepository.save(userFromDB);

        this.stubForUpdateGroup();

        User user = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(invitation)
                .post("/guests/api/invitations")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals("admin@utrecht.nl", user.getEduPersonPrincipalName());
    }

    @Test
    void postExistingUserNoDuplicateRoles() {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", INVITATION_HASH);
        invitation.put("status", Status.ACCEPTED);

        User user = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(invitation)
                .post("/guests/api/invitations")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals("admin@utrecht.nl", user.getEduPersonPrincipalName());
    }

    @Test
    void put() throws Exception {
        Role role = roleRepository.findAll().get(0);
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        Invitation invitation = new Invitation(Authority.GUEST,
                "Please accept",
                "guest@example.com",
                true,
                Collections.singleton(new InvitationRole(role)));
        invitation.setExpiryDate(Instant.now());
        Institution institution = getInstitution(user);
        invitation.setInstitution(institution);
        InvitationRequest invitationRequest = new InvitationRequest(invitation, Arrays.asList("guest@example.com", "admin@example.com"), institution.getId());

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(invitationRequest)
                .put("/guests/api/invitations")
                .then()
                .statusCode(201);
        assertEquals(4, invitationRepository.count());
    }
}