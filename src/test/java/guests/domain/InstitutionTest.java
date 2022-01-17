package guests.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InstitutionTest {

    @Test
    void incrementAup() {
        Institution institution = new Institution();
        institution.incrementAup();
        assertNull(institution.getAupVersion());

        institution.setAupUrl("https://aup");
        institution.incrementAup();
        assertEquals(1, institution.getAupVersion());
    }

    @Test
    void invariantAupVersion() {
        Institution institution = new Institution();
        institution.invariantAupVersion();
        assertNull(institution.getAupUrl());

        institution.setAupUrl("https://aup");
        institution.invariantAupVersion();
        assertEquals(1, institution.getAupVersion());
    }
}