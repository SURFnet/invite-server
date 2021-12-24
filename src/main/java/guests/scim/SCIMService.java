package guests.scim;

import com.fasterxml.jackson.databind.ObjectMapper;
import guests.domain.*;
import guests.exception.NotFoundException;
import guests.mail.MailBox;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SCIMService {

    private final ParameterizedTypeReference<Map<String, Object>> mapParameterizedTypeReference = new ParameterizedTypeReference<>() {
    };

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final MailBox mailBox;
    private final String groupUrnPrefix;
    private final String userAPI = "users";
    private final String groupAPI = "groups";

    @Autowired
    public SCIMService(ObjectMapper objectMapper, MailBox mailBox, @Value("${voot.group_urn_domain}") String groupUrnDomain) {
        this.objectMapper = objectMapper;
        this.mailBox = mailBox;
        this.groupUrnPrefix = String.format("urn:collab:group:%s", groupUrnDomain);
        // Otherwise, we can use method PATCH
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(5, TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(true);
        restTemplate.setRequestFactory(new OkHttp3ClientHttpRequestFactory(builder.build()));
    }

    @SneakyThrows
    public void newUserRequest(User user) {
        String userRequest = prettyJson(new UserRequest(user));
        getApplicationsFromUserRoles(user).forEach(application -> {
            UserRole userRole = userRoles(user, application);
            this.newRequest(application, userRequest, userAPI, userRole);
        });
    }

    @SneakyThrows
    public void updateUserRequest(User user) {
        getApplicationsFromUserRoles(user).forEach(application -> {
            UserRole userRole = userRoles(user, application);
            String userRequest = prettyJson(new UserRequest(user, userRole));
            this.updateRequest(application, userRequest, groupAPI, userRole);
        });
    }

    @SneakyThrows
    public void deleteUserRequest(User user) {
        getApplicationsFromUserRoles(user).forEach(application -> {
            UserRole userRole = userRoles(user, application);
            String userRequest = prettyJson(new UserRequest(user, userRole));
            this.deleteRequest(application, userRequest, userAPI, userRole);
        });
    }

    public void newRoleRequest(Role role) {
        if (role.getApplication().provisioningEnabled()) {
            String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
            String groupRequest = prettyJson(new GroupRequest(externalId, role.getDisplayName(), Collections.emptyList()));
            this.newRequest(role.getApplication(), groupRequest, groupAPI, role);
        }
    }

    public void updateRoleRequest(Role role, List<User> users) {
        if (role.getApplication().provisioningEnabled()) {
            String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
            List<Member> members = users.stream().map(user -> new Member(this.serviceProviderId(user, role))).collect(Collectors.toList());
            String groupRequest = prettyJson(new GroupRequest(externalId, role.getDisplayName(), members));
            this.updateRequest(role.getApplication(), groupRequest, groupAPI, role);
        }
    }

    public void deleteRolesRequest(Role role) {
        if (role.getApplication().provisioningEnabled()) {
            String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
            String groupRequest = prettyJson(new GroupRequest(externalId, role.getDisplayName(), Collections.emptyList()));
            this.deleteRequest(role.getApplication(), groupRequest, groupAPI, role);
        }
    }

    @SneakyThrows
    private void newRequest(Application application, String request, String apiType, ServiceProviderIdentifier serviceProviderIdentifier) {
        if (hasEmailHook(application)) {
            mailBox.sendProvisioningMail(String.format("SCIM %s: CREATE", apiType), request, application.getProvisioningHookEmail());
        } else {
            URI uri = this.provisioningUri(application, apiType, Optional.empty());
            RequestEntity<String> requestEntity = new RequestEntity<>(request, httpHeaders(application), HttpMethod.POST, uri);
            Map<String, Object> results = restTemplate.exchange(requestEntity, mapParameterizedTypeReference).getBody();
            String id = (String) results.get("id");
            serviceProviderIdentifier.setServiceProviderId(id);
        }
    }

    @SneakyThrows
    private void updateRequest(Application application, String request, String apiType, ServiceProviderIdentifier serviceProviderIdentifier) {
        if (hasEmailHook(application)) {
            mailBox.sendProvisioningMail(String.format("SCIM %s: UPDATE", apiType), request, application.getProvisioningHookEmail());
        } else {
            URI uri = this.provisioningUri(application, userAPI, Optional.of(serviceProviderIdentifier));
            RequestEntity<String> requestEntity = new RequestEntity<>(request, httpHeaders(application), HttpMethod.PATCH, uri);
            restTemplate.exchange(requestEntity, mapParameterizedTypeReference);
        }
    }

    @SneakyThrows
    private void deleteRequest(Application application, String request, String apiType, ServiceProviderIdentifier serviceProviderIdentifier) {
        if (hasEmailHook(application)) {
            mailBox.sendProvisioningMail(String.format("SCIM %s: DELETE", apiType), request, application.getProvisioningHookEmail());
        } else {
            URI uri = this.provisioningUri(application, userAPI, Optional.of(serviceProviderIdentifier));
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(application.getProvisioningHookUsername(), application.getProvisioningHookPassword());
            RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.DELETE, uri);
            restTemplate.exchange(requestEntity, Void.class);
        }
    }


    private String serviceProviderId(User user, Role role) {
        UserRole userRole = user.getRoles().stream()
                .filter(r -> r.getRole().getId().equals(role.getId()))
                .findFirst().orElseThrow(NotFoundException::new);
        return userRole.getServiceProviderId();
    }

    private boolean hasEmailHook(Application application) {
        return StringUtils.hasText(application.getProvisioningHookEmail());
    }

    private URI provisioningUri(Application application, String objectType, Optional<ServiceProviderIdentifier> spIdentifier) {
        String postFix = spIdentifier.map(role -> "/" + role.getServiceProviderId()).orElse("");
        return URI.create(String.format("%s/v1/%s%s",
                application.getProvisioningHookUrl(),
                objectType,
                postFix));
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
