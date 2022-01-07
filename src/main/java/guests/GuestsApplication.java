package guests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.audit.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.trace.http.HttpTraceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.trace.http.HttpTraceEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        HttpTraceAutoConfiguration.class,
        MetricsAutoConfiguration.class,
        AuditAutoConfiguration.class,
        HttpTraceEndpointAutoConfiguration.class})
public class GuestsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuestsApplication.class, args);
    }

}
