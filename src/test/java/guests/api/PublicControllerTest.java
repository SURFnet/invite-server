package guests.api;

import guests.AbstractTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicControllerTest extends AbstractTest {

    @Test
    void oidc() {
        Map<String, String> results = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap("state", "http://localhost"))
                .post("/guests/api/public/authorize")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getMap(".", String.class, String.class);
        assertTrue(results.containsKey("codeVerifier"));

        String url = results.get("authorizationUrl");
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(url).build().getQueryParams();
        assertEquals("http://localhost", URLDecoder.decode(parameters.getFirst("state"), Charset.defaultCharset()));
    }

}