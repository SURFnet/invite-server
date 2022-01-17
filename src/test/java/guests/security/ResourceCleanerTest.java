package guests.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import guests.AbstractTest;
import guests.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.Period;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceCleanerTest extends AbstractTest {

    @Autowired
    private ResourceCleaner subject;

    @Test
    void cleanUsers() throws JsonProcessingException {
        long beforeUsers = userRepository.count();
        markUser();
        stubForDeleteUser();
        stubForUpdateRole();
        subject.clean();
        assertEquals(beforeUsers, userRepository.count() + 1);
    }

    @Test
    void cleanUserRoles() throws JsonProcessingException {
        long beforeUserRoles = userRoleRepository.count();
        markUserRole();
        stubForUpdateRole();
        subject.clean();
        assertEquals(beforeUserRoles, userRoleRepository.count() + 1);
    }

    @Test
    void notCronJobResponsible() {
        ResourceCleaner resourceCleaner = new ResourceCleaner(null, null, null, 1, false);
        resourceCleaner.clean();
    }

    private void markUser() {
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("guest@utrecht.nl").get();
        Instant past = Instant.now().minus(Period.ofDays(1050));
        user.setLastActivity(past);
        user.getUserRoles().forEach(userRole -> userRole.setEndDate(past));
        userRepository.save(user);
    }

    private void markUserRole() {
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase("admin@utrecht.nl").get();
        Instant past = Instant.now().minus(Period.ofDays(1050));
        user.getUserRoles().forEach(userRole -> userRole.setEndDate(past));
        userRepository.save(user);
    }

}