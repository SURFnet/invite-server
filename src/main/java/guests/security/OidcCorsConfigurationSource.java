package guests.security;

import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OidcCorsConfigurationSource implements CorsConfigurationSource {

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        List<String> allAllowed = Collections.singletonList(CorsConfiguration.ALL);

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(allAllowed);
        corsConfiguration.setAllowedHeaders(allAllowed);
        corsConfiguration.setAllowedMethods(allAllowed);
        corsConfiguration.setAllowedHeaders(allAllowed);
        corsConfiguration.setMaxAge(1800L);
        corsConfiguration.setAllowCredentials(true);
        return corsConfiguration;
    }
}
