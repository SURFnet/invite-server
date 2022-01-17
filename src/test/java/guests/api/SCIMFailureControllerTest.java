package guests.api;

import guests.AbstractTest;
import guests.domain.*;
import guests.scim.GroupRequest;
import guests.scim.GroupURN;
import guests.scim.Member;
import guests.scim.UserRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static guests.scim.SCIMService.GROUP_API;
import static guests.scim.SCIMService.USER_API;
import static io.restassured.RestAssured.given;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

class SCIMFailureControllerTest extends AbstractTest {

    @Test
    void failures() throws IOException {
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
    void failureCount() throws IOException {
        Institution institution = seedSCIMFailure().getApplication().getInstitution();
        Map results = given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("institutionId", institution.getId())
                .get("/guests/api/scim/institution/{institutionId}/count")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getMap(".");
        assertEquals(results.get("count"), 1);
    }

    @Test
    void failureById() throws IOException {
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
    void failureByIdNotAllowed() throws IOException {
        SCIMFailure scimFailure = seedSCIMFailure();
        Institution institution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("admin@utrecht.nl", "introspect.json"))
                .pathParam("id", scimFailure.getId())
                .pathParam("institutionId", institution.getId())
                .get("/guests/api/scim/id/{id}/{institutionId}")
                .then()
                .statusCode(403);
    }

    @Test
    void failureByIdSuperAdminAllowed() throws IOException {
        SCIMFailure scimFailure = seedSCIMFailure();
        Institution institution = institutionRepository.findByHomeInstitutionIgnoreCase("utrecht.nl").get();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("id", scimFailure.getId())
                .pathParam("institutionId", institution.getId())
                .get("/guests/api/scim/id/{id}/{institutionId}")
                .then()
                .statusCode(200);
    }

    @Test
    void deleteFailure() throws IOException {
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

    @Test
    void resendSCIMFailureCreateUser() throws IOException {
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        UserRole userRole = user.getRoles().iterator().next();

        assertNotNull(userRole.getServiceProviderId());

        SCIMFailure scimFailure = new SCIMFailure(
                objectMapper.writeValueAsString(new UserRequest(user)),
                USER_API,
                HttpMethod.POST.name(),
                "http://localhost:8081/scim/v1/users",
                user.getEduPersonPrincipalName(),
                userRole.getRole().getApplication()
        );
        scimFailureRepository.save(scimFailure);

        stubForCreateUser();

        this.doResendSCIMFailure(scimFailure, "j.doe@example.com", true);

        UserRole userRoleAfter = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get().getRoles().iterator().next();

        assertNotNull(userRoleAfter.getServiceProviderId());
        assertNotEquals(userRole.getServiceProviderId(), userRoleAfter.getServiceProviderId());
    }

    @Test
    void resendSCIMFailureUpdateUser() throws IOException {
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        UserRole userRole = user.getRoles().iterator().next();
        SCIMFailure scimFailure = new SCIMFailure(
                objectMapper.writeValueAsString(new UserRequest(user)),
                USER_API,
                HttpMethod.PATCH.name(),
                "http://localhost:8081/scim/v1/users",
                user.getEduPersonPrincipalName(),
                userRole.getRole().getApplication()
        );
        scimFailureRepository.save(scimFailure);

        stubForUpdateUser();

        this.doResendSCIMFailure(scimFailure, "j.doe@example.com", true);
    }

    @Test
    void resendSCIMFailureDeleteUser() throws IOException {
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        UserRole userRole = user.getRoles().iterator().next();
        SCIMFailure scimFailure = new SCIMFailure(
                objectMapper.writeValueAsString(new UserRequest(user)),
                USER_API,
                HttpMethod.DELETE.name(),
                "http://localhost:8081/scim/v1/users",
                user.getEduPersonPrincipalName(),
                userRole.getRole().getApplication()
        );
        scimFailureRepository.save(scimFailure);

        stubForDeleteUser();

        this.doResendSCIMFailure(scimFailure, "j.doe@example.com", true);
    }

    @Test
    void resendSCIMFailureCreateUserEndpointDown() throws IOException {
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        UserRole userRole = user.getRoles().iterator().next();

        assertNotNull(userRole.getServiceProviderId());

        SCIMFailure scimFailure = new SCIMFailure(
                objectMapper.writeValueAsString(new UserRequest(user)),
                USER_API,
                HttpMethod.POST.name(),
                "http://localhost:8081/scim/v1/users",
                user.getEduPersonPrincipalName(),
                userRole.getRole().getApplication()
        );
        scimFailureRepository.save(scimFailure);
        // Do not stub for SCIM endpoint
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken("j.doe@example.com", "introspect.json"))
                .pathParam("id", scimFailure.getId())
                .pathParam("institutionId", scimFailure.getApplication().getInstitution().getId())
                .put("/guests/api/scim/id/{id}/{institutionId}")
                .then()
                .statusCode(500);
        assertEquals(1, scimFailureRepository.count());
    }

    @Test
    void resendSCIMFailureCreateRole() throws IOException {
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        Role role = user.getRoles().iterator().next().getRole();
        assertNotNull(role.getServiceProviderId());

        String externalId = GroupURN.urnFromRole("groupUrnPrefix", role);

        SCIMFailure scimFailure = new SCIMFailure(
                objectMapper.writeValueAsString(new GroupRequest(externalId, role.getName())),
                GROUP_API,
                HttpMethod.POST.name(),
                "http://localhost:8081/scim/v1/groups",
                externalId,
                role.getApplication()
        );
        scimFailureRepository.save(scimFailure);

        stubForCreateRole();

        this.doResendSCIMFailure(scimFailure, "j.doe@example.com", true);

        Role roleAfter = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get().getRoles().iterator().next().getRole();

        assertNotNull(roleAfter.getServiceProviderId());
        assertNotEquals(role.getServiceProviderId(), roleAfter.getServiceProviderId());
    }

    @Test
    void resendSCIMFailureUpdateRole() throws IOException {
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        UserRole userRole = user.getRoles().iterator().next();
        Role role = userRole.getRole();

        String externalId = GroupURN.urnFromRole("groupUrnPrefix", role);

        List<Member> members = userRoleRepository.findByRoleId(role.getId()).stream()
                .map(u -> new Member(userRole.getServiceProviderId()))
                .collect(Collectors.toList());
        SCIMFailure scimFailure = new SCIMFailure(
                objectMapper.writeValueAsString(new GroupRequest(externalId, role, role.getName(),  members)),
                GROUP_API,
                HttpMethod.PATCH.name(),
                "http://localhost:8081/scim/v1/groups",
                role.getServiceProviderId(),
                role.getApplication()
        );
        scimFailureRepository.save(scimFailure);

        stubForUpdateRole();

        this.doResendSCIMFailure(scimFailure, "j.doe@example.com", true);
    }

    @Test
    void resendSCIMFailureDeleteRole() throws IOException {
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        UserRole userRole = user.getRoles().iterator().next();
        Role role = userRole.getRole();

        String externalId = GroupURN.urnFromRole("groupUrnPrefix", role);

        SCIMFailure scimFailure = new SCIMFailure(
                objectMapper.writeValueAsString(new GroupRequest(externalId, role, role.getName(), emptyList())),
                GROUP_API,
                HttpMethod.DELETE.name(),
                "http://localhost:8081/scim/v1/groups",
                user.getEduPersonPrincipalName(),
                role.getApplication()
        );
        scimFailureRepository.save(scimFailure);

        stubForDeleteRole();

        this.doResendSCIMFailure(scimFailure, "j.doe@example.com", true);
    }

    @Test
    void resendSCIMFailureUnknownUserHttpMethod() throws IOException {
        SCIMFailure scimFailure = new SCIMFailure(
                null,
                USER_API,
                HttpMethod.OPTIONS.name(),
                "http://localhost:8081/scim/v1/users",
                null,
                applicationRepository.findByEntityIdIgnoreCase("canvas").get()
        );
        scimFailureRepository.save(scimFailure);
        this.doResendSCIMFailure(scimFailure, "j.doe@example.com", false);
    }

    @Test
    void resendSCIMFailureUnknownGroupHttpMethod() throws IOException {
        SCIMFailure scimFailure = new SCIMFailure(
                null,
                GROUP_API,
                HttpMethod.OPTIONS.name(),
                "http://localhost:8081/scim/v1/groups",
                null,
                applicationRepository.findByEntityIdIgnoreCase("canvas").get()
        );
        scimFailureRepository.save(scimFailure);
        this.doResendSCIMFailure(scimFailure, "j.doe@example.com", false);
    }

    @Test
    void resendSCIMFailureUnknownApi() throws IOException {
        SCIMFailure scimFailure = new SCIMFailure(
                null,
                "nope",
                HttpMethod.POST.name(),
                "http://localhost:8081/scim/v1/groups",
                null,
                applicationRepository.findByEntityIdIgnoreCase("canvas").get()
        );
        scimFailureRepository.save(scimFailure);
        this.doResendSCIMFailure(scimFailure, "j.doe@example.com", false);
    }

    private SCIMFailure seedSCIMFailure() {
        User user = seedUser();
        Application application = user.getRoles().iterator().next().getRole().getApplication();
        SCIMFailure scimFailure = new SCIMFailure("message", "groups", HttpMethod.POST.toString(), "https://failure", "serviceProviderId", application);
        return scimFailureRepository.save(scimFailure);
    }

    private void doResendSCIMFailure(SCIMFailure scimFailure, String eppn, boolean success) throws IOException {
        Institution institution = scimFailure.getApplication().getInstitution();
        given()
                .when()
                .accept(ContentType.JSON)
                .auth().oauth2(opaqueAccessToken(eppn, "introspect.json"))
                .pathParam("id", scimFailure.getId())
                .pathParam("institutionId", institution.getId())
                .put("/guests/api/scim/id/{id}/{institutionId}")
                .then()
                .statusCode(success ? 201 : 500);
        assertEquals(success ? 0 : 1, scimFailureRepository.count());
    }
}