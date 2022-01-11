package guests.api;

import guests.AbstractTest;
import guests.domain.Application;
import guests.domain.Institution;
import guests.domain.SCIMFailure;
import guests.domain.User;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SCIMFailureControllerTest extends AbstractTest {

    @Test
    void failures() {
        Institution institution = seedSCIMFailure().getApplication().getInstitution();
        List<Map> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/guests/api/scim/institution/{institutionId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Map.class);
        assertEquals(1, results.size());
        assertEquals("message", results.get(0).get("message"));
    }

    @Test
    void failureById() {
        SCIMFailure scimFailure = seedSCIMFailure();
        Institution institution = scimFailure.getApplication().getInstitution();
        Map results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("id", scimFailure.getId())
                .pathParam("institutionId", institution.getId())
                .get("/guests/api/scim/id/{id}/{institutionId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getMap(".");
        assertEquals("message", results.get("message"));
    }

    @Test
    void deleteFailure() {
        SCIMFailure scimFailure = seedSCIMFailure();
        Institution institution = scimFailure.getApplication().getInstitution();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("id", scimFailure.getId())
                .pathParam("institutionId", institution.getId())
                .delete("/guests/api/scim/id/{id}/{institutionId}")
                .then()
                .statusCode(201);
        assertEquals(0, scimFailureRepository.count());
    }

    private SCIMFailure seedSCIMFailure() {
        User user = seedUser();
        Application application = user.getRoles().iterator().next().getRole().getApplication();
        SCIMFailure scimFailure = new SCIMFailure("message", "groups", HttpMethod.POST.toString(), "https://failure", "serviceProviderId", application);
        return scimFailureRepository.save(scimFailure);
    }
}