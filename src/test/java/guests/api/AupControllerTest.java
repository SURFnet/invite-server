package guests.api;

import guests.AbstractTest;
import guests.domain.User;
import guests.exception.NotFoundException;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AupControllerTest extends AbstractTest {

    @Test
    void agreeAup() {
        User inviter = userRepository.findByEduPersonPrincipalNameIgnoreCase("inviter@utrecht.nl").orElseThrow(NotFoundException::new);
        assertEquals(0, inviter.getAups().size());

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .body(Collections.singletonList(inviter.getInstitutionMemberships().iterator().next().getInstitution().getId()))
                .put("/guests/api/aups")
                .then()
                .statusCode(201);

        inviter = userRepository.findByEduPersonPrincipalNameIgnoreCase("inviter@utrecht.nl").orElseThrow(NotFoundException::new);
        assertEquals(1, inviter.getAups().size());
    }

    @Test
    void agreeAupAlreadyPresent() {
        User admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").orElseThrow(NotFoundException::new);
        assertEquals(1, admin.getAups().size());

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(Collections.singletonList(admin.getInstitutionMemberships().iterator().next().getInstitution().getId()))
                .put("/guests/api/aups")
                .then()
                .statusCode(201);

        admin = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").orElseThrow(NotFoundException::new);
        assertEquals(1, admin.getAups().size());
    }
}