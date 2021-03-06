package guests.domain;

import guests.exception.InvalidProvisioningException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationTest {

    @Test
    void provisioningBothUrlEmail() {
        Assertions.assertThrows(InvalidProvisioningException.class, () -> {
            Application application = new Application();
            application.setProvisioningHookUrl("https://provisioning");
            application.setProvisioningHookUsername("inviter");
            application.setProvisioningHookPassword("secret");
            application.setProvisioningHookEmail("jdoe@example.com");
            application.validateProvisioning();
        });
    }

    @Test
    void provisioningNoPassword() {
        Assertions.assertThrows(InvalidProvisioningException.class, () -> {
            Application application = new Application();
            application.setProvisioningHookUrl("https://provisioning");
            application.setProvisioningHookUsername("user");
            application.validateProvisioning();
        });
    }

    @Test
    void provisioningValid() {
        Application application = new Application();
        application.setProvisioningHookUrl("https://provisioning");
        application.setProvisioningHookUsername("user");
        application.setProvisioningHookPassword("secret");
        application.validateProvisioning();
        assertTrue(application.provisioningEnabled());
    }

    @Test
    void provisioningEmptyApplicationValid() {
        Application application = new Application();
        application.validateProvisioning();
    }

    @Test
    void provisioningEmailValid() {
        Application application = new Application();
        application.setProvisioningHookEmail("hook@app.nl");
        application.validateProvisioning();
        assertTrue(application.provisioningEnabled());
    }

}