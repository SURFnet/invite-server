package guests.security;


import guests.domain.User;
import guests.repository.UserRepository;
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

    @Autowired
    public ResourceCleaner(UserRepository userRepository,
                           SCIMService scimService,
                           @Value("${cron.last-activity-duration-days}") int lastActivityDurationDays,
                           @Value("${cron.node-cron-job-responsible}") boolean cronJobResponsible) {
        this.userRepository = userRepository;
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
        Instant past = Instant.now().minus(Period.ofDays(lastActivityDurationDays));
        List<User> users = userRepository.findByLastActivityBefore(past);
        if (users.isEmpty()) {
            LOG.info(String.format("No users deleted with no activity in the last %s days", lastActivityDurationDays));
        } else {
            users.forEach(scimService::deleteUserRequest);
            userRepository.deleteAll(users);
            LOG.info(String.format("Deleted users with no activity in the last %s days: %s ",
                    lastActivityDurationDays,
                    users.stream().map(User::getEduPersonPrincipalName).collect(Collectors.toList())));
        }
    }

}
