package guests.api;

import guests.AbstractTest;
import guests.domain.Institution;
import guests.domain.ObjectExists;
import io.restassured.http.ContentType;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InstitutionControllerTest extends AbstractTest {

    @Test
    @SuppressWarnings("unchecked")
    void institutions() throws IOException {
        List<Institution> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .get("/api/v1/institutions")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Institution.class);
        assertEquals(3, results.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void mine() throws IOException {
        List<Institution> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .get("/api/v1/institutions/mine")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Institution.class);
        assertEquals(1, results.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void institutionById() throws IOException {
        Long id = institutionRepository.findByEntityIdIgnoreCase("https://utrecht").get().getId();
        Institution institution = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .get("/api/v1/institutions/{id}", id)
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", Institution.class);
        assertEquals("https://utrecht", institution.getEntityId());
    }


    @Test
    @SuppressWarnings("unchecked")
    void existingInstitutionEntityIdNotExists() throws IOException {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(true, "https://ut"))
                .post("/api/v1/institutions/entity-id-exists")
                .then()
                .body("exists", IsEqual.equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void institutionEntityIdNotExists() throws IOException {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(false, "nope"))
                .post("/api/v1/institutions/entity-id-exists")
                .then()
                .body("exists", IsEqual.equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void institutionEntityIdExists() throws IOException {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(false, "https://uva"))
                .post("/api/v1/institutions/entity-id-exists")
                .then()
                .body("exists", IsEqual.equalTo(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    void existingInstitutionSchacHomeNotExists() throws IOException {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(true, "uva.nl"))
                .post("/api/v1/institutions/schac-home-exists")
                .then()
                .body("exists", IsEqual.equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void institutionSchacHomeNotExists() throws IOException {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(false, "nope"))
                .post("/api/v1/institutions/schac-home-exists")
                .then()
                .body("exists", IsEqual.equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void institutionSchacHomeExists() throws IOException {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(false, "uva.nl"))
                .post("/api/v1/institutions/schac-home-exists")
                .then()
                .body("exists", IsEqual.equalTo(true));
    }

    @Test
    void save() throws IOException {
        Institution institution = new Institution("displayName", "entityId", "home.nl", "https://aup", null);
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(institution)
                .post("/api/v1/institutions")
                .then()
                .statusCode(201);
        institution = institutionRepository.findByHomeInstitutionIgnoreCase("home.nl").get();
        assertEquals(1, institution.getAupVersion());
    }

    @Test
    void saveWithoutAupURL() throws IOException {
        Institution institution = new Institution("displayName", "entityId", "home.nl", null, 5);
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(institution)
                .post("/api/v1/institutions")
                .then()
                .statusCode(201);
        institution = institutionRepository.findByHomeInstitutionIgnoreCase("home.nl").get();
        assertNull(institution.getAupVersion());
    }

    @Test
    void update() throws Exception {
        Institution institution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl").get();
        institution.setDisplayName("Changed");
        //We mimic the client behaviour
        Map<String, Object> institutionMap = this.convertObjectToMap(institution);
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(institutionMap)
                .put("/api/v1/institutions")
                .then()
                .statusCode(201);
        institution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl").get();
        assertEquals("Changed", institution.getDisplayName());
    }

    @Test
    void incrementAup() throws Exception {
        Institution institution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("id", institution.getId())
                .put("/api/v1/institutions/increment-aup/{id}")
                .then()
                .statusCode(201);
        institution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl").get();
        assertEquals(2, institution.getAupVersion());
    }

    @Test
    void updateInstitutionAdmin() throws Exception {
        Institution institution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl").get();
        institution.setEntityId("https://changed");
        institution.setHomeInstitution("changed");
        //We mimic the client behaviour
        Map<String, Object> institutionMap = this.convertObjectToMap(institution);
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(institutionMap)
                .put("/api/v1/institutions")
                .then()
                .statusCode(201);
        institution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl").get();
        assertEquals("https://utrecht", institution.getEntityId());
        assertEquals("utrecht.nl", institution.getHomeInstitution());
    }

    @Test
    void deleteInstitution() throws IOException {
        Institution institution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl").get();
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("id", institution.getId())
                .delete("/api/v1/institutions/{id}")
                .then()
                .statusCode(201);
        Optional<Institution> optionalInstitution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl");
        assertEquals(false, optionalInstitution.isPresent());
    }

    @Test
    void deleteInstitutionNotAllowed() throws IOException {
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("id", 1)
                .delete("/api/v1/institutions/{id}")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteApplicationUnknownUser() throws IOException {
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("nope@surf.nl", "introspect.json"))
                .pathParam("id", 1)
                .delete("/api/v1/institutions/{id}")
                .then()
                .statusCode(403);
    }

}