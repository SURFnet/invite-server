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
        User user = seedUser();
        Application application = user.getRoles().iterator().next().getRole().getApplication();
        SCIMFailure scimFailure = new SCIMFailure("message", "groups", HttpMethod.POST.toString(), "https://failure", "serviceProviderId", application);
        scimFailureRepository.save(scimFailure);

        Institution institution = application.getInstitution();
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
}