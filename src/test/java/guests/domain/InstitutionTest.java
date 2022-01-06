package guests.domain;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InstitutionTest {

    @Test
    void incrementAup() {
        Institution institution = new Institution();
        institution.incrementAup();
        assertNull(institution.getAupVersion());

        institution.setAupUrl("https://aup");
        institution.invariantAupVersion(true);
        institution.incrementAup();
        assertEquals("2", institution.getAupVersion());
    }
}