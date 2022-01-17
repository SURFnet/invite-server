package guests.scim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadLocalSCIMFailureStrategyTest {

    @Test
    void context() {
        assertFalse(ThreadLocalSCIMFailureStrategy.ignoreFailures());

        ThreadLocalSCIMFailureStrategy.startIgnoringFailures();
        assertTrue(ThreadLocalSCIMFailureStrategy.ignoreFailures());

        ThreadLocalSCIMFailureStrategy.stopIgnoringFailures();
        assertFalse(ThreadLocalSCIMFailureStrategy.ignoreFailures());

        ThreadLocalSCIMFailureStrategy.stopIgnoringFailures();

    }

}