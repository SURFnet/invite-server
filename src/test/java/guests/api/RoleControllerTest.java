package guests.api;

import guests.AbstractTest;
import guests.domain.Application;
import guests.domain.Institution;
import guests.domain.Role;
import guests.domain.RoleExists;
import io.restassured.http.ContentType;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RoleControllerTest extends AbstractTest {

    @Test
    void rolesByInstitution() throws IOException {
        Institution institution = institutionRepository.findByEntityIdIgnoreCase("https://utrecht").get();
        List<Map> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/api/v1/roles/institution/{institutionId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Map.class);
        assertEquals(2, results.size());
    }

    @Test
    void rolesByApplication() throws IOException {
        Application application = applicationRepository.findByEntityIdIgnoreCase("canvas").get();
        List<Role> results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("applicationId", application.getId())
                .get("/api/v1/roles/application/{applicationId}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", Role.class);
        assertEquals(2, results.size());
    }

    @Test
    void createRole() throws Exception {
        Application application = applicationRepository.findByEntityIdIgnoreCase("canvas").get();
        //We mimic the client behaviour
        Role role = new Role("test_role", application);
        Map<String, Object> roleMap = convertObjectToMap(role);
        roleMap.put("application", Collections.singletonMap("id", application.getId()));

        this.stubForCreateRole();

        Role newRole = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(roleMap)
                .post("/api/v1/roles")
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
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(roleMap)
                .post("/api/v1/roles")
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
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(roleMap)
                .post("/api/v1/roles")
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
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .body(roleMap)
                .post("/api/v1/roles")
                .then()
                .statusCode(404);
    }

    @Test
    void deleteRole() throws IOException {
        Role role = roleRepository.findAll().get(0);

        this.stubForDeleteRole();

        given()
                .when()
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("id", role.getId())
                .delete("/api/v1/roles/{id}")
                .then()
                .statusCode(201);
        Optional<Role> optionalRole = roleRepository.findById(role.getId());
        assertEquals(false, optionalRole.isPresent());
    }

    @Test
    void deleteRoleNotAllowed() throws IOException {
        Role role = roleRepository.findAll().get(0);
        given()
                .when()
                .auth().oauth2(opaqueAccessToken("inviter@utrecht.nl", "introspect.json"))
                .pathParam("id", role.getId())
                .delete("/api/v1/roles/{id}")
                .then()
                .statusCode(403);
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
                .post("/api/v1/roles/name-exists")
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
                .post("/api/v1/roles/name-exists")
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
                .body(new RoleExists(false, "administratorCanvas", application.getId()))
                .post("/api/v1/roles/name-exists")
                .then()
                .body("exists", IsEqual.equalTo(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRole() throws IOException {
        Role role = roleRepository.findAll().get(0);
        Map<String, Object> result = given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("id", role.getId())
                .get("/api/v1/roles/{id}")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getMap(".");
        assertEquals(result.get("name"), role.getName());
        assertNotNull(((Map) ((Map) result.get("application")).get("institution")).get("id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRoleNotAllowed() throws IOException {
        Role role = roleRepository.findAll().get(0);
        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("guest@utrecht.nl", "introspect.json"))
                .pathParam("id", role.getId())
                .get("/api/v1/roles/{id}")
                .then()
                .statusCode(403);
    }
}