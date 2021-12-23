package guests.scim;

import com.fasterxml.jackson.databind.ObjectMapper;
import guests.domain.Application;
import guests.domain.Role;
import guests.domain.User;
import guests.domain.UserRole;
import guests.mail.MailBox;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SCIMService {

    private final ParameterizedTypeReference<Map<String, Object>> mapParameterizedTypeReference = new ParameterizedTypeReference<>() {
    };

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final MailBox mailBox;

    @Autowired
    public SCIMService(ObjectMapper objectMapper, MailBox mailBox) {
        this.objectMapper = objectMapper;
        this.mailBox = mailBox;
    }

    @SneakyThrows
    public boolean newUserRequest(User user) {
        Collection<Application> applications = getApplicationsFromUserRoles(user);
        String userRequest = prettyJson(new UserRequest(user));
        applications.forEach(application -> {
            if (StringUtils.hasText(application.getProvisioningHookEmail())) {
                mailBox.sendProvisioningMail(userRequest, application.getProvisioningHookEmail());
            } else {
                RequestEntity<String> requestEntity = new RequestEntity<>(userRequest, httpHeaders(application), HttpMethod.POST, URI.create(application.getProvisioningHookUrl()));
                Map<String, Object> results = restTemplate.exchange(requestEntity, mapParameterizedTypeReference).getBody();
                String id = (String) results.get("id");
                //update the correct user role
                UserRole userRole = userRoles(user, application);
                userRole.setServiceProviderId(id);
            }
        });
        return !applications.isEmpty();
    }

    @SneakyThrows
    public boolean updateUserRequest(User user) {
        Collection<Application> applications = getApplicationsFromUserRoles(user);
        applications.forEach(application -> {
            UserRole userRole = userRoles(user, application);
            String userRequest = prettyJson(new UserRequest(user, userRole));
            if (StringUtils.hasText(application.getProvisioningHookEmail())) {
                mailBox.sendProvisioningMail(userRequest, application.getProvisioningHookEmail());
            } else {
                RequestEntity<String> requestEntity = new RequestEntity<>(userRequest, httpHeaders(application), HttpMethod.PATCH, URI.create(application.getProvisioningHookUrl()));
                restTemplate.exchange(requestEntity, mapParameterizedTypeReference);
            }
        });
        return !applications.isEmpty();
    }

    private Collection<Application> getApplicationsFromUserRoles(User user) {
        //prevent duplicate provisioning of different roles to the same application
        return user.getRoles().stream()
                .map(userRole -> userRole.getRole().getApplication())
                .filter(Application::provisioningEnabled)
                .collect(Collectors.toMap(Application::getId, app -> app)).values();
    }

    private UserRole userRoles(User user, Application application) {
        return user.getRoles().stream()
                .filter(userRole -> userRole.getRole().getApplication().getId().equals(application.getId()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    @SneakyThrows
    private String prettyJson(Object obj) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    private HttpHeaders httpHeaders(Application application) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(application.getProvisioningHookUsername(), application.getProvisioningHookPassword());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

}
