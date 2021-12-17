package guests.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import guests.config.SuperAdmin;
import guests.config.UserHandlerMethodArgumentResolver;
import guests.repository.InstitutionRepository;
import guests.repository.UserRepository;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

@EnableWebSecurity
public class SecurityConfig {

    @Configuration
    @Order(1)
    @EnableConfigurationProperties(SuperAdmin.class)
    public static class JWTSecurityConfig extends WebSecurityConfigurerAdapter {

        @Value("${oidc.introspection_uri}")
        private String introspectionUri;

        @Value("${oidc.resource_server_id}")
        private String clientId;

        @Value("${oidc.rs_secret}")
        private String secret;

        private final InstitutionRepository institutionRepository;

        private final UserRepository userRepository;
        private final ObjectMapper objectMapper;
        private final SuperAdmin superAdmin;

        public JWTSecurityConfig(InstitutionRepository institutionRepository,
                                 UserRepository userRepository,
                                 ObjectMapper objectMapper,
                                 SuperAdmin superAdmin) {
            this.institutionRepository = institutionRepository;
            this.userRepository = userRepository;
            this.objectMapper = objectMapper;
            this.superAdmin = superAdmin;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            String json = IOUtils.toString(new ClassPathResource("securityMatrix.json").getInputStream(), Charset.defaultCharset());
            Map<String, Map<String, String>> securityMap = objectMapper.readValue(json, new TypeReference<>() {
            });
            http.cors().configurationSource(new OidcCorsConfigurationSource()).configure(http);
            http
                    .requestMatchers()
                    .antMatchers("/guests/api/**")
                    .and()
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .csrf().disable()
                    .httpBasic().disable()
                    .anonymous()
                    .and()
                    .authorizeRequests(authz -> authz
                            .antMatchers("/guests/api/public/**", "/guests/api/validations/**")
                            .permitAll())
                    .addFilterAfter(
                            new UserAuthenticationFilter(institutionRepository, userRepository, new SecurityMatrix(securityMap), superAdmin),
                            FilterSecurityInterceptor.class)
                    .authorizeRequests(authz -> authz
                            .antMatchers("/guests/api/**").hasAuthority("SCOPE_openid")
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.opaqueToken(token -> token
                            .introspectionUri(introspectionUri)
                            .introspectionClientCredentials(clientId, secret)));
        }
    }

    @Configuration
    public class MvcConfig implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
            argumentResolvers.add(new UserHandlerMethodArgumentResolver());
        }
    }
}