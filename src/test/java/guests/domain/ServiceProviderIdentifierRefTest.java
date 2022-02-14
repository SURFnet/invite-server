package guests.domain;

import guests.AbstractTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceProviderIdentifierRefTest {

    @Test
    void setServiceProviderId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ServiceProviderIdentifierRef("test").setServiceProviderId("nope"));
    }

    @Test
    void getServiceProviderId() {
        assertEquals("test", new ServiceProviderIdentifierRef("test").getServiceProviderId());
    }
}