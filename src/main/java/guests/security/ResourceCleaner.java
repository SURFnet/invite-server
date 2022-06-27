package guests.security;


import guests.domain.User;
import guests.domain.UserRole;
import guests.repository.UserRepository;
import guests.repository.UserRoleRepository;
import guests.scim.OperationType;
import guests.scim.SCIMService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ResourceCleaner {

    private static Log LOG = LogFactory.getLog(ResourceCleaner.class);

    private final UserRepository userRepository;
    private final boolean cronJobResponsible;
    private final int lastActivityDurationDays;
    private final SCIMService scimService;
    private final UserRoleRepository userRoleRepository;

    @Autowired
    public ResourceCleaner(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           SCIMService scimService,
                           @Value("${cron.last-activity-duration-days}") int lastActivityDurationDays,
                           @Value("${cron.node-cron-job-responsible}") boolean cronJobResponsible) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.lastActivityDurationDays = lastActivityDurationDays;
        this.cronJobResponsible = cronJobResponsible;
        this.scimService = scimService;
    }

    @Scheduled(cron = "${cron.user-cleaner-expression}")
    @Transactional
    public void clean() {
        if (!cronJobResponsible) {
            return;
        }
        cleanUsers();
        cleanUserRoles();
    }

    private void cleanUsers() {
        Instant past = Instant.now().minus(Period.ofDays(lastActivityDurationDays));
        List<User> users = userRepository.findByLastActivityBefore(past);

        LOG.info(String.format("Deleted %s users with no activity in the last %s days: %s ",
                users.size(),
                lastActivityDurationDays,
                users.stream().map(User::getEduPersonPrincipalName).collect(Collectors.toList())));

        users.forEach(scimService::deleteUserRequest);
        userRepository.deleteAll(users);
    }

    private void cleanUserRoles() {
        List<UserRole> userRoles = userRoleRepository.findByEndDateBefore(Instant.now());

        LOG.info(String.format("Deleted %s userRoles with an endDate in the past: %s",
                userRoles.size(),
                userRoles.stream()
                        .map(userRole -> String.format("%s - %s", userRole.getUser().getEduPersonPrincipalName(), userRole.getRole().getName()))
                        .collect(Collectors.toList())));

        userRoles.forEach(userRole -> scimService.updateRoleRequest(userRole, OperationType.Remove));

        userRoles.forEach(userRole -> {
            User user = userRole.getUser();
            user.removeUserRole(userRole);
            userRepository.save(user);
        });


    }

}
