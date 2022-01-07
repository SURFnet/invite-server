package guests.security;

import guests.config.SuperAdmin;
import guests.config.UserHandlerMethodArgumentResolver;
import guests.repository.InstitutionRepository;
import guests.repository.UserRepository;
import guests.scim.SCIMService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@EnableWebSecurity
@EnableScheduling
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
        private final SuperAdmin superAdmin;
        private final SCIMService scimService;

        public JWTSecurityConfig(InstitutionRepository institutionRepository,
                                 UserRepository userRepository,
                                 SuperAdmin superAdmin,
                                 SCIMService scimService) {
            this.institutionRepository = institutionRepository;
            this.userRepository = userRepository;
            this.superAdmin = superAdmin;
            this.scimService = scimService;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
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
                            new UserAuthenticationFilter(institutionRepository, userRepository, superAdmin, scimService),
                            FilterSecurityInterceptor.class)
                    .authorizeRequests(authz -> authz
                            .antMatchers("/guests/api/**").hasAuthority("SCOPE_openid")
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.opaqueToken(token -> token
                            .introspector(new CachingOpaqueTokenIntrospector(introspectionUri, clientId, secret))));
        }
    }

    @Configuration
    public class MvcConfig implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
            argumentResolvers.add(new UserHandlerMethodArgumentResolver());
        }
    }

    @Configuration
    @Order(2)
    public static class AppSecurity extends WebSecurityConfigurerAdapter {

        private @Value("${voot.user}")
        String user;
        private @Value("${voot.password}")
        String password;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .requestMatchers()
                    .antMatchers("/api/voot/**")
                    .and()
                    .csrf()
                    .disable()
                    .authorizeRequests()
                    .antMatchers("/api/voot/**")
                    .authenticated()
                    .and()
                    .httpBasic()
                    .and()
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> builder = auth
                    .inMemoryAuthentication()
                    .withUser(user)
                    .password("{noop}" + password)
                    .roles("openid")
                    .and();

        }
    }

}