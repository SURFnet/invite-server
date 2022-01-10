package guests.api;

import guests.AbstractTest;
import guests.domain.Application;
import guests.domain.ApplicationExists;
import guests.domain.Institution;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationControllerTest extends AbstractTest {

    @Test
    @SuppressWarnings("unchecked")
    void applicationById() {
        Long id = applicationRepository.findByEntityIdIgnoreCase("blackboard").get().getId();
        Application application = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .get("/guests/api/applications/{id}", id)
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", Application.class);
        assertEquals("blackboard", application.getEntityId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void applicationByIdNotAllowed() {
        Long id = applicationRepository.findByEntityIdIgnoreCase("blackboard").get().getId();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .get("/guests/api/applications/{id}", id)
                .then()
                .statusCode(403);
    }

    @Test
    @SuppressWarnings("unchecked")
    void applicationsInvalidToken() {
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect-invalid-token.json"))
                .get("/guests/api/applications")
                .then().statusCode(401);
    }

    @Test
    void update() throws Exception {
        Application application = applicationRepository.findByEntityIdIgnoreCase("canvas").get();
        application.setDisplayName("Changed");
        //We mimic the client behaviour
        Map<String, Object> appMap = this.convertObjectToMap(application);
        appMap.put("institution", Collections.singletonMap("id", application.getInstitution().getId()));
        appMap.put("provisioningHookPassword", "Changed");
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(appMap)
                .put("/guests/api/applications")
                .then()
                .statusCode(201);
        application = applicationRepository.findByEntityIdIgnoreCase("canvas").get();
        assertEquals("Changed", application.getDisplayName());
        assertEquals("Changed", application.getProvisioningHookPassword());
    }

    @Test
    void updateUserRestriction() throws Exception {
        Application application = applicationRepository.findByEntityIdIgnoreCase("BLACKBOARD").get();
        application.setDisplayName("Changed");
        //We mimic the client behaviour
        Map<String, Object> appMap = this.convertObjectToMap(application);
        appMap.put("institution", Collections.singletonMap("id", application.getInstitution().getId()));
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(appMap)
                .put("/guests/api/applications")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteApplication() {
        Application application = applicationRepository.findByEntityIdIgnoreCase("canvas").get();
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("id", application.getId())
                .delete("/guests/api/applications/{id}")
                .then()
                .statusCode(201);
        Optional<Application> optionalApplication = applicationRepository.findByEntityIdIgnoreCase("canvas");
        assertEquals(false, optionalApplication.isPresent());
    }

    @Test
    void deleteApplicationNotAllowed() {
        //Institution admin of other institution
        Application application = applicationRepository.findByEntityIdIgnoreCase("blackboard").get();
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("mary@utrecht.nl", "introspect.json"))
                .pathParam("id", application.getId())
                .delete("/guests/api/applications/{id}")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteApplicationNotAllowedByInviter() {
        Application application = applicationRepository.findByEntityIdIgnoreCase("canvas").get();
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .pathParam("id", application.getId())
                .delete("/guests/api/applications/{id}")
                .then()
                .statusCode(403);
    }

    @Test
    @SuppressWarnings("unchecked")
    void applicationsForUser() {
        List<Application> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .get("/guests/api/applications/user")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Application.class);
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getRoles().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void applicationsFoInstitution() {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://utrecht").get();
        List<Application> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/guests/api/applications/institution/{institutionId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Application.class);
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getRoles().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void existingApplicationEntityIdNotExists() {
        Application application = applicationRepository.findByEntityIdIgnoreCase("blackboard").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ApplicationExists(true, "blackboard", application.getInstitution().getId()))
                .post("/guests/api/applications/entity-id-exists")
                .then()
                .body("exists", equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void applicationEntityIdNotExists() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ApplicationExists(false, "nope", 1L))
                .post("/guests/api/applications/entity-id-exists")
                .then()
                .body("exists", equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void applicationEntityIdExists() {
        Application application = applicationRepository.findByEntityIdIgnoreCase("blackboard").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ApplicationExists(false, "blackboard", application.getInstitution().getId()))
                .post("/guests/api/applications/entity-id-exists")
                .then()
                .body("exists", equalTo(true));
    }

}