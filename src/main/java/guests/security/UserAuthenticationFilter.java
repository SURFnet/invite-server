package guests.security;

import guests.config.SuperAdmin;
import guests.domain.Authority;
import guests.domain.Institution;
import guests.domain.User;
import guests.repository.InstitutionRepository;
import guests.repository.UserRepository;
import guests.scim.SCIMService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.transaction.annotation.Transactional;
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

    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final SecurityMatrix securityMatrix;
    private final SuperAdmin superAdmin;
    private final SCIMService scimService;

    public UserAuthenticationFilter(InstitutionRepository institutionRepository,
                                    UserRepository userRepository,
                                    SecurityMatrix securityMatrix,
                                    SuperAdmin superAdmin,
                                    SCIMService scimService) {
        this.institutionRepository = institutionRepository;
        this.userRepository = userRepository;
        this.securityMatrix = securityMatrix;
        this.superAdmin = superAdmin;
        this.scimService = scimService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requestURI = request.getRequestURI();
        if (authentication instanceof AnonymousAuthenticationToken &&
                (requestURI.startsWith("/guests/api/public") || requestURI.startsWith("/guests/api/validations"))) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        if (!(authentication instanceof BearerTokenAuthentication)) {
            responseForbidden(servletResponse);
            return;
        }
        BearerTokenAuthentication tokenAuthentication = (BearerTokenAuthentication) authentication;
        String httpMethod = request.getMethod().toLowerCase();
        String edupersonPrincipalName = (String) tokenAuthentication.getTokenAttributes().get("eduperson_principal_name");
        Optional<User> optionalUser = userRepository.findByEduPersonPrincipalNameIgnoreCase(edupersonPrincipalName);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            boolean allowed = securityMatrix.isAllowed(requestURI, httpMethod, user);
            if (allowed) {
                tokenAuthentication.setDetails(user);
                if (user.hasChanged(tokenAuthentication.getTokenAttributes())) {
                    scimService.updateUserRequest(user);
                }
                user.setLastActivity(Instant.now());
                userRepository.save(user);
                filterChain.doFilter(servletRequest, servletResponse);
            } else {
                responseForbidden(servletResponse);
            }
        } else {
            Optional<String> optionalEppn = superAdmin.getUsers().stream().filter(eppn -> eppn.equalsIgnoreCase(edupersonPrincipalName)).findAny();
            if (optionalEppn.isPresent()) {
                User user = new User(this.getOrProvisionInstitution(), Authority.SUPER_ADMIN, tokenAuthentication.getTokenAttributes());
                userRepository.save(user);
                tokenAuthentication.setDetails(user);
                filterChain.doFilter(servletRequest, servletResponse);
            } else if (requestURI.startsWith("/guests/api/invitations") && (httpMethod.equals("post") || httpMethod.equals("get"))) {
                filterChain.doFilter(servletRequest, servletResponse);
            } else {
                responseForbidden(servletResponse);
            }
        }
    }

    private void responseForbidden(ServletResponse servletResponse) {
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
