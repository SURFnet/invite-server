package guests.api;

import guests.AbstractTest;
import guests.domain.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest extends AbstractTest {

    @Test
    @SuppressWarnings("unchecked")
    void get() throws IOException {
        User user = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .get("/api/v1/users/me")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals(Authority.SUPER_ADMIN, user.getInstitutionMemberships().iterator().next().getAuthority());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getWithoutToken() {
        given()
                .when()
                .accept(ContentType.JSON)
                .get("/api/v1/users/me")
                .then()
                .statusCode(401);
    }

    @Test
    @SuppressWarnings("unchecked")
    void allByInstitution() throws IOException {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://utrecht").get();
        List<User> users = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/api/v1/users/institution/{institutionId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", User.class);
        assertEquals(3, users.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void allByInstitutionFiltered() throws IOException {
        //First we need a user which is member of two institutions and then verify we only get one
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("hash", INVITATION_UVA_HASH);
        invitation.put("status", Status.ACCEPTED);

        this.stubForCreateUser();
        this.stubForUpdateRole();

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("guest@utrecht.nl", "introspect.json"))
                .body(invitation)
                .post("/api/v1/invitations")
                .then()
                .statusCode(201);

        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://utrecht").get();
        List<User> users = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/api/v1/users/institution/{institutionId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", User.class);
        assertEquals(3, users.size());

        User guest = users.stream().filter(user -> user.getEduPersonPrincipalName().equals("guest@utrecht.nl")).findFirst().get();
        assertEquals(1, guest.getInstitutionMemberships().size());

        assertEquals(2, userRepository.findById(guest.getId()).get().getInstitutionMemberships().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void emailsByInstitution() throws IOException {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://utrecht").get();
        List<Map> res = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/api/v1/users/emails/{institutionId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Map.class);
        assertEquals(1, res.size());
        assertEquals(3, res.get(0).size());
        assertEquals("guest@utrecht.nl", res.get(0).get("email"));
        assertEquals("fn", res.get(0).get("given_name"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void allByApplication() throws IOException {
        Application application = applicationRepository.findByEntityIdIgnoreCase("CANVAS").get();
        List<User> users = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("applicationId", application.getId())
                .get("/api/v1/users/application/{applicationId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", User.class);
        assertEquals(2, users.size());
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
                .get("/api/v1/users/institution/{institutionId}")
                .then()
                .statusCode(403);
    }

    @Test
    void delete() throws IOException {
        super.stubForDeleteUser();
        super.stubForUpdateRole();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .delete("/api/v1/users")
                .then()
                .statusCode(201);
        assertFalse(userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getWithRoles() throws IOException {
        User user = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .get("/api/v1/users/me")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals(1, user.getUserRoles().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void other() throws IOException {
        User inviter = userRepository.findByEduPersonPrincipalNameIgnoreCase("inviter@utrecht.nl").get();
        User user = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("userId", inviter.getId())
                .get("/api/v1/users/{userId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals("inviter@utrecht.nl", user.getEduPersonPrincipalName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void otherNotAllowed() throws IOException {
        User admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .pathParam("userId", admin.getId())
                .get("/api/v1/users/{userId}")
                .then()
                .statusCode(403);
    }

    @Test
    @SuppressWarnings("unchecked")
    void otherAllowedByInviter() throws IOException {
        User inviter = userRepository.findByEduPersonPrincipalNameIgnoreCase("guest@utrecht.nl").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .pathParam("userId", inviter.getId())
                .get("/api/v1/users/{userId}")
                .then()
                .statusCode(200);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteOtherAllowedByInviter() throws IOException {
        User inviter = userRepository.findByEduPersonPrincipalNameIgnoreCase("guest@utrecht.nl").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .pathParam("userId", inviter.getId())
                .delete("/api/v1/users/{userId}")
                .then()
                .statusCode(201);
        assertTrue(userRepository.findByEduPersonPrincipalNameIgnoreCase("guest@utrecht.nl").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteOtherNotAllowed() throws IOException {
        User admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .pathParam("userId", admin.getId())
                .delete("/api/v1/users/{userId}")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteOther() throws IOException {
        super.stubForDeleteUser();
        super.stubForUpdateRole();
        User inviter = userRepository.findByEduPersonPrincipalNameIgnoreCase("inviter@utrecht.nl").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("userId", inviter.getId())
                .delete("/api/v1/users/{userId}")
                .then()
                .statusCode(201);
        assertFalse(userRepository.findByEduPersonPrincipalNameIgnoreCase("inviter@utrecht.nl").isPresent());
    }

    @Test
    void deleteOtherUserRole() throws IOException {
        super.stubForUpdateRole();
        User admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        UserRole userRole = admin.getUserRoles().iterator().next();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("userId", admin.getId())
                .pathParam("userRoleId", userRole.getId())
                .delete("/api/v1/users/role/{userId}/{userRoleId}")
                .then()
                .statusCode(201);

        admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        assertEquals(0, admin.getUserRoles().size());
    }

    @Test
    void deleteOtherUserRoleNotAllowed() throws IOException {
        super.stubForUpdateRole();
        User admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        UserRole userRole = admin.getUserRoles().iterator().next();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("guest@utrecht.nl", "introspect.json"))
                .pathParam("userId", admin.getId())
                .pathParam("userRoleId", userRole.getId())
                .delete("/api/v1/users/role/{userId}/{userRoleId}")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteOtherInstitutionMembership() throws IOException {
        super.stubForUpdateRole();
        User admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        InstitutionMembership membership = admin.getInstitutionMemberships().iterator().next();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("userId", admin.getId())
                .pathParam("membershipId", membership.getId())
                .delete("/api/v1/users/membership/{userId}/{membershipId}")
                .then()
                .statusCode(201);

        admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        assertEquals(0, admin.getInstitutionMemberships().size());
    }

    @Test
    void deleteOtherNonSuperAdmin() throws IOException {
        super.stubForUpdateRole();
        User admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("guest@utrecht.nl").get();
        InstitutionMembership membership = admin.getInstitutionMemberships().iterator().next();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("userId", admin.getId())
                .pathParam("membershipId", membership.getId())
                .delete("/api/v1/users/membership/{userId}/{membershipId}")
                .then()
                .statusCode(201);

        admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("guest@utrecht.nl").get();
        assertEquals(0, admin.getInstitutionMemberships().size());
    }

    @Test
    void deleteOtherInstitutionMembershipNotAllowed() throws IOException {
        super.stubForUpdateRole();
        User admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        InstitutionMembership membership = admin.getInstitutionMemberships().iterator().next();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("guest@utrecht.nl", "introspect.json"))
                .pathParam("userId", admin.getId())
                .pathParam("membershipId", membership.getId())
                .delete("/api/v1/users/membership/{userId}/{membershipId}")
                .then()
                .statusCode(403);
    }
}