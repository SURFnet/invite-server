package guests.api;

import guests.AbstractTest;
import guests.domain.Institution;
import guests.domain.ObjectExists;
import io.restassured.http.ContentType;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InstitutionControllerTest extends AbstractTest {

    @Test
    @SuppressWarnings("unchecked")
    void institutions() {
        List<Institution> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .get("/guests/api/institutions")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Institution.class);
        assertEquals(3, results.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void mine() {
        List<Institution> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .get("/guests/api/institutions/mine")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Institution.class);
        assertEquals(1, results.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void institutionById() {
        Long id = institutionRepository.findByEntityIdIgnoreCase("https://utrecht").get().getId();
        Institution institution = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .get("/guests/api/institutions/{id}", id)
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", Institution.class);
        assertEquals("https://utrecht", institution.getEntityId());
    }


    @Test
    @SuppressWarnings("unchecked")
    void existingInstitutionEntityIdNotExists() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(true, "https://ut"))
                .post("/guests/api/institutions/entity-id-exists")
                .then()
                .body("exists", IsEqual.equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void institutionEntityIdNotExists() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(false, "nope"))
                .post("/guests/api/institutions/entity-id-exists")
                .then()
                .body("exists", IsEqual.equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void institutionEntityIdExists() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(false, "https://uva"))
                .post("/guests/api/institutions/entity-id-exists")
                .then()
                .body("exists", IsEqual.equalTo(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    void existingInstitutionSchacHomeNotExists() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(true, "uva.nl"))
                .post("/guests/api/institutions/schac-home-exists")
                .then()
                .body("exists", IsEqual.equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void institutionSchacHomeNotExists() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(false, "nope"))
                .post("/guests/api/institutions/schac-home-exists")
                .then()
                .body("exists", IsEqual.equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void institutionSchacHomeExists() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new ObjectExists(false, "uva.nl"))
                .post("/guests/api/institutions/schac-home-exists")
                .then()
                .body("exists", IsEqual.equalTo(true));
    }

    @Test
    void save() {
        Institution institution = new Institution("displayName", "entityId", "home.nl", "https://aup", null);
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(institution)
                .post("/guests/api/institutions")
                .then()
                .statusCode(201);
        institution = institutionRepository.findByHomeInstitutionIgnoreCase("home.nl").get();
        assertEquals(1, institution.getAupVersion());
    }

    @Test
    void saveWithoutAupURL() {
        Institution institution = new Institution("displayName", "entityId", "home.nl", null, 5);
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(institution)
                .post("/guests/api/institutions")
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
                .put("/guests/api/institutions")
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
                .put("/guests/api/institutions/increment-aup/{id}")
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
                .put("/guests/api/institutions")
                .then()
                .statusCode(201);
        institution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl").get();
        assertEquals("https://utrecht", institution.getEntityId());
        assertEquals("utrecht.nl", institution.getHomeInstitution());
    }

    @Test
    void deleteInstitution() {
        Institution institution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl").get();
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("id", institution.getId())
                .delete("/guests/api/institutions/{id}")
                .then()
                .statusCode(201);
        Optional<Institution> optionalInstitution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl");
        assertEquals(false, optionalInstitution.isPresent());
    }

    @Test
    void deleteInstitutionNotAllowed() {
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("id", 1)
                .delete("/guests/api/institutions/{id}")
                .then()
                .statusCode(403);
    }

    @Test
    void deleteApplicationUnknownUser() {
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("nope@surf.nl", "introspect.json"))
                .pathParam("id", 1)
                .delete("/guests/api/institutions/{id}")
                .then()
                .statusCode(403);
    }

}