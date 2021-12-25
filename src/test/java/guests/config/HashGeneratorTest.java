package guests.config;

import guests.AbstractTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashGeneratorTest {

    @Test
    void generateHash() {
        String hash = new HashGenerator().generateHash();
        assertTrue(hash.length() > 128);
    }
}