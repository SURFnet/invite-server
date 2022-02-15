package guests.api;

import guests.AbstractTest;
import guests.domain.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class InvitationControllerTest extends AbstractTest {

    @Test
    void get() throws IOException {
        Invitation invitation = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("unknown@user.nl", "introspect.json"))
                .pathParam("hash", INVITATION_UTRECHT_HASH)
                .get("/api/v1/invitations/{hash}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", Invitation.class);
        assertEquals(Status.OPEN, invitation.getStatus());
        assertEquals("administratorCanvas", invitation.getRoles().iterator().next().getRole().getName());
    }

    @Test
    void getExistingUser() throws IOException {
        Map<String, Object> invitation = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("hash", INVITATION_EMAIL_EQUALITY_HASH)
                .get("/api/v1/invitations/{hash}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getMap(".");
        assertEquals(true, invitation.get("emailEqualityConflict"));
    }

    @Test
    void getById() throws IOException {
        Long id = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get().getId();
        Invitation invitation = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .pathParam("id", id)
                .get("/api/v1/invitations/id/{id}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", Invitation.class);
        assertEquals(Status.OPEN, invitation.getStatus());
        assertEquals("administratorCanvas", invitation.getRoles().iterator().next().getRole().getName());
    }

    @Test
    void getByIdNotAllowed() throws IOException {
        Long id = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get().getId();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("guest@utrecht.nl", "introspect.json"))
                .pathParam("id", id)
                .get("/api/v1/invitations/id/{id}")
                .then()
                .statusCode(403);
    }

    @Test
    @SuppressWarnings("unchecked")
    void allByInstitution() throws IOException {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://utrecht").get();
        List<Invitation> invitations = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/api/v1/invitations/institution/{institutionId}")
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
                .get("/api/v1/invitations/application/{applicationId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Invitation.class);
        assertEquals(2, invitations.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void allByInstitutionNotAllowed() throws IOException {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://uva").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/api/v1/invitations/institution/{institutionId}")
                .then()
                .statusCode(403);
    }

    @Test
    void post() throws IOException {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", INVITATION_UTRECHT_HASH);
        invitation.put("status", Status.ACCEPTED);

        this.stubForCreateUser();
        this.stubForUpdateRole();

        User user = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("new@user.nl", "introspect.json"))
                .body(invitation)
                .post("/api/v1/invitations")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals("new@user.nl", user.getEduPersonPrincipalName());
    }

    @Test
    void postEmailInequality() throws IOException {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", INVITATION_EMAIL_EQUALITY_HASH);
        invitation.put("status", Status.ACCEPTED);

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("new@user.nl", "introspect.json"))
                .body(invitation)
                .post("/api/v1/invitations")
                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void postUnspecifiedUrnConflict() throws IOException {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", INVITATION_UTRECHT_HASH);
        invitation.put("status", Status.ACCEPTED);

        User inviter = userRepository.findByEduPersonPrincipalNameIgnoreCase("inviter@utrecht.nl").get();
        inviter.setUnspecifiedId("new@user.nl");
        userRepository.save(inviter);

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("new@user.nl", "introspect.json"))
                .body(invitation)
                .post("/api/v1/invitations")
                .then()
                .statusCode(HttpStatus.PRECONDITION_FAILED.value());
    }

    @Test
    void postExistingUser() throws IOException {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", INVITATION_UTRECHT_HASH);
        invitation.put("status", Status.ACCEPTED);
        //prevent user_roles_unique_user_role exception
        User userFromDB = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        userFromDB.setUserRoles(new HashSet<>());
        userRepository.save(userFromDB);

        this.stubForUpdateRole();

        User user = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(invitation)
                .post("/api/v1/invitations")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals("admin@utrecht.nl", user.getEduPersonPrincipalName());
        assertEquals(1, user.getUserRoles().size());
    }

    @Test
    void postExistingUserNoDuplicateRoles() throws IOException {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", INVITATION_UTRECHT_HASH);
        invitation.put("status", Status.ACCEPTED);

        User user = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(invitation)
                .post("/api/v1/invitations")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals("admin@utrecht.nl", user.getEduPersonPrincipalName());
        assertEquals(1, user.getUserRoles().size());
    }

    @Test
    void postExistingUserNewMembership() throws IOException {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", INVITATION_UTRECHT_HASH);
        invitation.put("status", Status.ACCEPTED);
        User userFromDB = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        userFromDB.setInstitutionMemberships(new HashSet<>());
        userRepository.save(userFromDB);

        User user = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(invitation)
                .post("/api/v1/invitations")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals("admin@utrecht.nl", user.getEduPersonPrincipalName());
        assertEquals(1, user.getInstitutionMemberships().size());
    }

    @Test
    void put() throws IOException {
        Role role = roleRepository.findAll().get(0);
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        Instant after90days = Instant.now().plus(90, ChronoUnit.DAYS);
        long beforeCount = invitationRepository.count();
        Invitation invitation = new Invitation(Authority.GUEST,
                "Please accept",
                "guest@example.com",
                true,
                Collections.singleton(new InvitationRole(role, after90days)));
        invitation.setExpiryDate(Instant.now());
        Institution institution = getInstitution(user);
        invitation.setInstitution(institution);
        InvitationRequest invitationRequest = new InvitationRequest(
                invitation,
                Arrays.asList("guest@example.com", "admin@example.com"),
                institution.getId());

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(invitationRequest)
                .put("/api/v1/invitations")
                .then()
                .statusCode(201);

        List<Invitation> invitationList = invitationRepository.findAll();
        assertEquals(beforeCount, invitationList.size() - invitationRequest.getInvites().size());

        InvitationRole invitationRole = invitationList.stream()
                .filter(inv -> inv.getEmail().equals("admin@example.com"))
                .findFirst()
                .get().getRoles()
                .iterator().next();
        assertEquals(after90days.toString().substring(0, 10),
                invitationRole.getEndDate().toString().substring(0, 10));
    }

    @Test
    void putNotAllowedAuthority() throws IOException {
        Role role = roleRepository.findAll().get(0);
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        Invitation invitation = new Invitation(Authority.INVITER,
                "Please accept",
                "guest@example.com",
                true,
                Collections.singleton(new InvitationRole(role)));
        invitation.setExpiryDate(Instant.now());
        Institution institution = getInstitution(user);
        invitation.setInstitution(institution);
        InvitationRequest invitationRequest = new InvitationRequest(invitation, Arrays.asList("guest@example.com"), institution.getId());

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .body(invitationRequest)
                .put("/api/v1/invitations")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteNotAllowed() throws IOException {
        Long id = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get().getId();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .pathParam("id", id)
                .delete("/api/v1/invitations/{id}")
                .then()
                .statusCode(403);
        assertTrue(invitationRepository.findById(id).isPresent());
    }

    @Test
    void delete() throws IOException {
        Long id = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get().getId();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("id", id)
                .delete("/api/v1/invitations/{id}")
                .then()
                .statusCode(201);
        assertFalse(invitationRepository.findById(id).isPresent());
    }

    @Test
    void resendNotAllowed() throws IOException {
        Long id = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get().getId();
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("id", id);
        invitation.put("message", "Please...");

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .body(invitation)
                .put("/api/v1/invitations/resend")
                .then()
                .statusCode(403);
    }

    @Test
    void resendAllowedInvitationIsForGuest() throws IOException {
        Invitation invitation = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get();
        invitation.setIntendedAuthority(Authority.GUEST);
        invitationRepository.save(invitation);

        Long id = invitation.getId();
        InvitationUpdate invitationUpdate = new InvitationUpdate();
        invitationUpdate.setId(id);
        invitationUpdate.setMessage("Please...");
        invitationUpdate.setExpiryDate(Instant.now().plus(14, ChronoUnit.DAYS));

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .body(invitationUpdate)
                .put("/api/v1/invitations/resend")
                .then()
                .statusCode(201);

        invitation = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get();

        assertEquals(invitationUpdate.getMessage(), invitation.getMessage());
        assertEquals(invitationUpdate.getExpiryDate().toString().substring(0, 10),
                invitation.getExpiryDate().toString().substring(0, 10));
    }

    @Test
    void updateInvitation() throws IOException {
        Invitation invitation = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get();
        Long id = invitation.getId();

        InvitationUpdate invitationUpdate = new InvitationUpdate();
        invitationUpdate.setId(id);
        invitationUpdate.setExpiryDate(Instant.now().plus(14, ChronoUnit.DAYS));

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(invitationUpdate)
                .put("/api/v1/invitations/update-expiry-date")
                .then()
                .statusCode(201);

        invitation = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get();
        assertEquals(invitationUpdate.getExpiryDate().toString().substring(0, 10),
                invitation.getExpiryDate().toString().substring(0, 10));
    }

    @Test
    void deleteAllowedByInviterGuestInvitation() throws IOException {
        Invitation invitation = invitationRepository.findByHashAndStatus(INVITATION_UTRECHT_HASH, Status.OPEN).get();
        invitation.setIntendedAuthority(Authority.GUEST);
        invitationRepository.save(invitation);

        Long id = invitation.getId();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .pathParam("id", id)
                .delete("/api/v1/invitations/{id}")
                .then()
                .statusCode(201);
        assertFalse(invitationRepository.findById(id).isPresent());
    }


}