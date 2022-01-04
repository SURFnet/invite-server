package guests.api;

import guests.AbstractTest;
import guests.domain.Validation;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;

class ValidationControllerTest extends AbstractTest {

    @Test
    void validateEmail() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("email", "jdoe@example.com"))
                .post("/guests/api/validations/validate")
                .then()
                .body("valid", equalTo(true));
    }

    @Test
    void validateEmptyEmail() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("email", ""))
                .post("/guests/api/validations/validate")
                .then()
                .body("valid", equalTo(true));
    }

    @Test
    void validateInvalidEmail() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("email", "nope"))
                .post("/guests/api/validations/validate")
                .then()
                .body("valid", equalTo(false));
    }

    @Test
    void validateUrl() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON).contentType(ContentType.JSON)
                .body(new Validation("url", "https://example.com"))
                .post("/guests/api/validations/validate")
                .then()
                .body("valid", equalTo(true));
    }

    @Test
    void validateInvalidUrl() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("url", "nope"))
                .post("/guests/api/validations/validate")
                .then()
                .body("valid", equalTo(false));
    }

    @Test
    void validateEmptyUrl() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("url", null))
                .post("/guests/api/validations/validate")
                .then()
                .body("valid", equalTo(true));
    }

    @Test
    void validateNope() {
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new Validation("nope", "value"))
                .post("/guests/api/validations/validate")
                .then()
                .statusCode(500);
    }

}