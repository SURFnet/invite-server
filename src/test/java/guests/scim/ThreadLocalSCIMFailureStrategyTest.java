package guests.scim;

import guests.AbstractTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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