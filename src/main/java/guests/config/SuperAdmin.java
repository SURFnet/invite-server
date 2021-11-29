package guests.config;

import guests.domain.Application;
import guests.domain.Institution;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "super-admin")
@Getter
@Setter
public class SuperAdmin {

    private Institution institution;
    private List<String> users;

}
