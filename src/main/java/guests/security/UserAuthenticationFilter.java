package guests.security;

import guests.config.SuperAdmin;
import guests.domain.Authority;
import guests.domain.Institution;
import guests.domain.InstitutionMembership;
import guests.domain.User;
import guests.repository.InstitutionRepository;
import guests.repository.UserRepository;
import guests.scim.SCIMService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

public class UserAuthenticationFilter extends GenericFilterBean {

    private static final Log LOG = LogFactory.getLog(UserAuthenticationFilter.class);

    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final SuperAdmin superAdmin;
    private final SCIMService scimService;

    public UserAuthenticationFilter(InstitutionRepository institutionRepository,
                                    UserRepository userRepository,
                                    SuperAdmin superAdmin,
                                    SCIMService scimService) {
        this.institutionRepository = institutionRepository;
        this.userRepository = userRepository;
        this.superAdmin = superAdmin;
        this.scimService = scimService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/guests/api/public") || requestURI.startsWith("/guests/api/validations")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        if (!(authentication instanceof BearerTokenAuthentication tokenAuthentication)) {
            responseForbidden(servletResponse, authentication, requestURI);
            return;
        }
        String httpMethod = request.getMethod().toLowerCase();
        String edupersonPrincipalName = (String) tokenAuthentication.getTokenAttributes().get("eduperson_principal_name");
        Optional<User> optionalUser = userRepository.findByEduPersonPrincipalNameIgnoreCase(edupersonPrincipalName);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            tokenAuthentication.setDetails(user);
            if (user.hasChanged(tokenAuthentication.getTokenAttributes())) {
                scimService.updateUserRequest(user);
            }
            user.setLastActivity(Instant.now());
            userRepository.save(user);
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            Optional<String> optionalEppn = superAdmin.getUsers().stream().filter(eppn -> eppn.equalsIgnoreCase(edupersonPrincipalName)).findAny();
            if (optionalEppn.isPresent()) {
                Institution institution = this.getOrProvisionInstitution();
                User user = new User(institution, Authority.SUPER_ADMIN, tokenAuthentication.getTokenAttributes());
                userRepository.save(user);
                tokenAuthentication.setDetails(user);
                filterChain.doFilter(servletRequest, servletResponse);
            } else if (requestURI.startsWith("/guests/api/invitations") && (httpMethod.equals("post") || httpMethod.equals("get"))) {
                filterChain.doFilter(servletRequest, servletResponse);
            } else {
                responseForbidden(servletResponse, authentication, requestURI);
            }
        }
    }

    private void responseForbidden(ServletResponse servletResponse, Authentication authentication, String requestURI) {
        LOG.warn(String.format("Returning 403 for authentication %s and requestURI %s", authentication, requestURI));
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        response.setHeader("Content-Type", "application/json");
        response.setStatus(HttpStatus.FORBIDDEN.value());
    }

    private Institution getOrProvisionInstitution() {
        final Institution institution = superAdmin.getInstitution();
        return institutionRepository.findByHomeInstitutionIgnoreCase(institution.getHomeInstitution())
                .orElseGet(() -> institutionRepository.save(new Institution(institution)));
    }
}
