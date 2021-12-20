package guests.voot;

import guests.AbstractTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class VootControllerTest extends AbstractTest {

    @Test
    void getGroupMemberships() {
        List<Map> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().basic("voot", "secret")
                .pathParam("unspecifiedId", "urn:collab:person:example.com:mdoe")
                .get("/api/voot/{unspecifiedId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Map.class);
        assertEquals(1, results.size());
        assertEquals(results.get(0).get("urn"), "urn:collab:group:test.eduid.nl:utrecht.nl:canvas:administrator");
    }

}
