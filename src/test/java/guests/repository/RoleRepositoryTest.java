package guests.repository;

import guests.AbstractTest;
import guests.domain.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleRepositoryTest extends AbstractTest {

    @Test
    void findByApplication_nameIgnoreCaseAndApplication_institution_homeInstitutionIgnoreCaseAndNameIgnoreCase() {
        Role role = roleRepository.findByApplication_institution_homeInstitutionIgnoreCaseAndApplication_nameIgnoreCaseAndNameIgnoreCase(
                "UTRECHT.nl", "canvas", "ADMINISTRATOR"
        ).get();
        assertEquals("administrator", role.getName());

        roleRepository.findByServiceProviderId(role.getServiceProviderId()).get();
    }

}