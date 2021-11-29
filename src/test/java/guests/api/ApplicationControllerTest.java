package guests.api;

import guests.AbstractTest;
import guests.domain.Application;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationControllerTest extends AbstractTest {

    @Test
    @SuppressWarnings("unchecked")
    void applications() throws Exception {
        List<Application> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .get("/guests/api/applications")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Application.class);
        assertEquals(2, results.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void applicationsInvalidToken() throws Exception {
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
        assertEquals("Changed", application.getDisplayName()    );
    }

    @Test
    void deleteApplication() throws Exception {
        Application application = applicationRepository.findByEntityIdIgnoreCase("canvas").get();
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("id", application.getId())
                .delete("/guests/api/applications/{id}")
                .then()
                .statusCode(204);
        Optional<Application> optionalApplication = applicationRepository.findByEntityIdIgnoreCase("canvas");
        assertEquals(false, optionalApplication.isPresent());
    }


}