package guests.domain;

import guests.AbstractTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameHolderListenerTest extends AbstractTest {

    @Test
    void beforeSave() {
        Institution institution = new Institution(
                "displayName",
                "https://entityId",
                " Ä ë &^%$#@!*- ok.Bę ",
                "https://aup", "1");
        institution = institutionRepository.save(institution);
        assertEquals("A_e__ok.Be", institution.getHomeInstitution());
    }
}