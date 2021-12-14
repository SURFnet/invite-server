package guests.api;

import guests.AbstractTest;
import guests.domain.Application;
import guests.domain.ObjectExists;
import guests.domain.Role;
import guests.domain.RoleExists;
import io.restassured.http.ContentType;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.*;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RoleControllerTest extends AbstractTest {

    @Test
    void roles() throws Exception {
        List<Role> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("mdoe@surf.nl", "introspect.json"))
                .get("/guests/api/roles")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Role.class);
        assertEquals(1, results.size());
    }

    @Test
    void createRole() throws Exception {
        Application application = applicationRepository.findByEntityIdIgnoreCase("canvas").get();
        //We mimic the client behaviour
        Role role = new Role("test_role", application);
        Map<String, Object> roleMap = convertObjectToMap(role);
        roleMap.put("application", Collections.singletonMap("id", application.getId()));
        Role newRole = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("mdoe@surf.nl", "introspect.json"))
                .body(roleMap)
                .post("/guests/api/roles")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getObject(".", Role.class);
        assertNotNull(newRole.getId());
    }

    @Test
    void createRoleWithWrongApplication() throws Exception {
        Application application = applicationRepository.findByEntityIdIgnoreCase("blackboard").get();
        Role role = new Role("test_role", application);
        Map<String, Object> roleMap = convertObjectToMap(role);
        roleMap.put("application", Collections.singletonMap("id", application.getId()));
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("mdoe@surf.nl", "introspect.json"))
                .body(roleMap)
                .post("/guests/api/roles")
                .then()
                .statusCode(403);
    }

    @Test
    void updateRole() throws Exception {
        Role role = roleRepository.findAll().get(0);
        role.setName("test_role");
        Map<String, Object> roleMap = convertObjectToMap(role);
        roleMap.put("application", Collections.singletonMap("id", role.getApplication().getId()));
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("mdoe@surf.nl", "introspect.json"))
                .body(roleMap)
                .post("/guests/api/roles")
                .then()
                .statusCode(201);
        role = roleRepository.findById(role.getId()).get();
        assertEquals("test_role", role.getName());
    }

    @Test
    void updateRoleApplicationMismatch() throws Exception {
        Role role = roleRepository.findAll().get(0);
        Map<String, Object> roleMap = convertObjectToMap(role);
        Map<String, Object> appMap = new HashMap<>();
        appMap.put("id", 999);
        roleMap.put("application", appMap);
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("mdoe@surf.nl", "introspect.json"))
                .body(roleMap)
                .post("/guests/api/roles")
                .then()
                .statusCode(404);
    }

    @Test
    void deleteRole() throws Exception {
        Role role = roleRepository.findAll().get(0);
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("mdoe@surf.nl", "introspect.json"))
                .pathParam("id", role.getId())
                .delete("/guests/api/roles/{id}")
                .then()
                .statusCode(204);
        Optional<Role> optionalRole = roleRepository.findById(role.getId());
        assertEquals(false, optionalRole.isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    void existingRoleNotExists() throws Exception {
        Application application = applicationRepository.findByEntityIdIgnoreCase("canvas").get();

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new RoleExists(true, "administrator", application.getId()))
                .post("/guests/api/roles/name-exists")
                .then()
                .body("exists", IsEqual.equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void roleNotExists() throws Exception {
        Application application = applicationRepository.findByEntityIdIgnoreCase("canvas").get();

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new RoleExists(false, "nope", application.getId()))
                .post("/guests/api/roles/name-exists")
                .then()
                .body("exists", IsEqual.equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void roleExists() throws Exception {
        Application application = applicationRepository.findByEntityIdIgnoreCase("canvas").get();

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .body(new RoleExists(false, "administrator", application.getId()))
                .post("/guests/api/roles/name-exists")
                .then()
                .body("exists", IsEqual.equalTo(true));
    }
}