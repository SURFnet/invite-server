package guests.voot;

import guests.AbstractTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VootControllerTest extends AbstractTest {

    @Test
    void getGroupMemberships() {
        List<Map> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().basic("voot", "secret")
                .pathParam("unspecifiedId", "admin@utrecht.nl")
                .get("/api/voot/{unspecifiedId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Map.class);
        assertEquals(1, results.size());
        assertEquals( "urn:collab:group:test.eduid.nl:utrecht.nl:canvas:administratorcanvas",results.get(0).get("urn"));
    }

    @Test
    void getEmptyGroupMemberships() {
        List<Map> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().basic("voot", "secret")
                .pathParam("unspecifiedId", "nope")
                .get("/api/voot/{unspecifiedId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Map.class);
        assertEquals(0, results.size());
    }

    @Test
    void getGroupMemberships401() {
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().basic("nope", "nope")
                .pathParam("unspecifiedId", "nope")
                .get("/api/voot/{unspecifiedId}")
                .then()
                .statusCode(401);
    }

}
