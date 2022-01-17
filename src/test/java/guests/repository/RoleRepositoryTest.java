package guests.repository;

import guests.AbstractTest;
import guests.domain.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleRepositoryTest extends AbstractTest {

    @Test
    void findByApplication_nameIgnoreCaseAndApplication_institution_homeInstitutionIgnoreCaseAndNameIgnoreCase() {
        Role role = roleRepository.findByApplication_institution_homeInstitutionIgnoreCaseAndApplication_nameIgnoreCaseAndNameIgnoreCase(
                "UTRECHT.nl", "canvas", "ADMINISTRATORcanvas"
        ).get();
        assertEquals("administratorCanvas", role.getName());

        roleRepository.findByServiceProviderId(role.getServiceProviderId()).get();
    }

}