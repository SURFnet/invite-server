package guests.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HashGeneratorTest {

    @Test
    void generateHash() {
        String hash = new HashGenerator().generateHash();
        assertTrue(hash.length() > 128);
    }
}