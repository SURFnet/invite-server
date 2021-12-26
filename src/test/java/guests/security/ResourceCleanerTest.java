package guests.security;

import guests.AbstractTest;
import guests.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.Period;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceCleanerTest extends AbstractTest {

    @Autowired
    private ResourceCleaner subject;

    @Test
    void clean() {
        long before = userRepository.count();
        markUser();
        stubForDeleteUser();

        subject.clean();
        assertEquals(before, userRepository.count() + 1);

        subject.clean();
        assertEquals(before, userRepository.count() + 1);
    }

    @Test
    void notCronJobResponsible() {
        ResourceCleaner resourceCleaner = new ResourceCleaner(null, null, 1, false);
        resourceCleaner.clean();
    }

    private void markUser() {
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        Instant past = Instant.now().minus(Period.ofDays(1050));
        user.setLastActivity(past);
        userRepository.save(user);
    }

}