package guests.api;

import guests.AbstractTest;
import guests.domain.Authority;
import guests.domain.Institution;
import guests.domain.User;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserControllerTest extends AbstractTest {

    @Test
    @SuppressWarnings("unchecked")
    void get() throws Exception {
        User user = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .get("/guests/api/users/me")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals(Authority.SUPER_ADMIN, user.getAuthority());
    }

    @Test
    @SuppressWarnings("unchecked")
    void allByInstitution() throws Exception {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://ut").get();
        List<User> users = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/guests/api/users/institution/{institutionId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", User.class);
        assertEquals(2, users.size());
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
                .get("/guests/api/users/institution/{institutionId}")
                .then()
                .statusCode(403);
    }

    @Test
    void delete() throws Exception {
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .delete("/guests/api/users")
                .then()
                .statusCode(204);
        assertFalse(userRepository.findByEduPersonPrincipalNameIgnoreCase("j.doe@example.com").isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getWithRoles() throws Exception {
        User user = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("mdoe@surf.nl", "introspect.json"))
                .get("/guests/api/users/me")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", User.class);
        assertEquals(1, user.getRoles().size());
    }

}