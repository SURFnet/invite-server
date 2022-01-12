package guests.scim;

import guests.domain.Application;
import guests.domain.Institution;
import guests.domain.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupURNTest {

    @Test
    void urnFromRole() {
        String groupPrefix = "urn:collab:group:test.eduid.nl";

        Institution institution = new Institution();
        institution.setHomeInstitution("HOME.NL");

        Application application = new Application();
        application.setName("APP_NAME");
        application.setInstitution(institution);

        Role role = new Role("ROLE_NAME", application);

        String urn = GroupURN.urnFromRole(groupPrefix, role);
        assertEquals("urn:collab:group:test.eduid.nl:home.nl:app_name:role_name", urn);
    }

    @Test
    void parseUrnRole() {
        ExternalID externalID = GroupURN.parseUrnRole("urn:collab:group:test.eduid.nl:HOME.NL:APP_NAME:ROLE_NAME");
        assertEquals("home.nl", externalID.institutionHome());
        assertEquals("app_name", externalID.applicationName());
        assertEquals("role_name", externalID.roleName());
    }
}