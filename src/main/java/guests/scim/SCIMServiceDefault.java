package guests.scim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import guests.domain.*;
import guests.exception.NotFoundException;
import guests.mail.MailBox;
import guests.repository.RoleRepository;
import guests.repository.SCIMFailureRepository;
import guests.repository.UserRepository;
import guests.repository.UserRoleRepository;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class SCIMServiceDefault implements SCIMService {

    public final static String USER_API = "users";
    public final static String GROUP_API = "groups";

    private static final Log LOG = LogFactory.getLog(SCIMServiceDefault.class);

    private final ParameterizedTypeReference<Map<String, Object>> mapParameterizedTypeReference = new ParameterizedTypeReference<>() {
    };

    private final ParameterizedTypeReference<String> stringParameterizedTypeReference = new ParameterizedTypeReference<>() {
    };

    private final RestTemplate restTemplate = new RestTemplate();

    private final SCIMFailureRepository scimFailureRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final ObjectMapper objectMapper;
    private final MailBox mailBox;
    private final String groupUrnPrefix;

    @Autowired
    public SCIMServiceDefault(SCIMFailureRepository scimFailureRepository,
                              UserRepository userRepository,
                              UserRoleRepository userRoleRepository,
                              RoleRepository roleRepository,
                              ObjectMapper objectMapper,
                              MailBox mailBox,
                              @Value("${voot.group_urn_domain}") String groupUrnDomain) {
        this.scimFailureRepository = scimFailureRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.objectMapper = objectMapper;
        this.mailBox = mailBox;
        this.groupUrnPrefix = String.format("urn:collab:group:%s", groupUrnDomain);
        // Otherwise, we can't use method PATCH
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(1, TimeUnit.MINUTES);
        builder.retryOnConnectionFailure(true);
        restTemplate.setRequestFactory(new OkHttp3ClientHttpRequestFactory(builder.build()));
    }

    @Override
    @SneakyThrows
    public void newUserRequest(User user) {
        Map<Application, List<UserRole>> applicationListMap = user.userRolesPerApplicationProvisioningEnabled();
        applicationListMap.forEach((application, userRoles) -> {
            /*
             * Contract of userRolesPerApplication is that there is at least one UserRole
             *
             * All userRoles in the same application have the same serviceProviderID
             */
            Optional<UserRole> userRoleProvisioned = getUserRoleProvisioned(userRoles);
            if (userRoleProvisioned.isPresent()) {
                UserRole userRole = userRoleProvisioned.get();
                userRoles.forEach(ur -> ur.setServiceProviderId(userRole.getServiceProviderId()));
            } else {
                UserRole userRole = userRoles.get(0);
                if (hasEmailHook(application)) {
                    String serviceProviderID = UUID.randomUUID().toString();
                    userRoles.forEach(ur -> ur.setServiceProviderId(serviceProviderID));
                    String mailUserRequest = prettyJson(new UserRequest(user, userRole));
                    this.newRequest(application, mailUserRequest, USER_API, userRole);
                } else {
                    String userRequest = prettyJson(new UserRequest(user));
                    this.newRequest(application, userRequest, USER_API, userRole);
                    userRoles.forEach(ur -> ur.setServiceProviderId(userRole.getServiceProviderId()));
                }
            }
        });
    }

    @Override
    @SneakyThrows
    public void updateUserRequest(User user) {
        user.userRolesPerApplicationProvisioningEnabled().forEach((application, userRoles) -> {
            Optional<UserRole> userRoleProvisioned = getUserRoleProvisioned(userRoles);
            if (userRoleProvisioned.isPresent()) {
                UserRole userRole = userRoleProvisioned.get();
                String userRequest = prettyJson(new UserRequest(user, userRole));
                this.updateRequest(application, userRequest, USER_API, userRole, HttpMethod.PUT);
            } else {
                this.newUserRequest(user);
            }
        });
    }

    @Override
    @SneakyThrows
    public void deleteUserRequest(User user) {
        //First send update role requests
        user.getUserRoles().forEach(userRole -> this.doUpdateRoleRequest(userRole, OperationType.Remove, user.getUserRoles()));

        user.userRolesPerApplicationProvisioningEnabled().forEach((application, userRoles) -> {
            Optional<UserRole> userRoleProvisioned = getUserRoleProvisioned(userRoles);
            if (userRoleProvisioned.isPresent()) {
                UserRole userRole = userRoleProvisioned.get();
                String userRequest = prettyJson(new UserRequest(user, userRole));
                this.deleteRequest(application, userRequest, USER_API, userRole);
            }
        });
    }

    @Override
    @SneakyThrows
    public void deleteUserByInstitutionRequest(User user, Institution institution) {
        //First send update role requests
        List<UserRole> userRolesToDelete = user.getUserRoles()
                .stream()
                .filter(role -> role.getRole().getInstitutionId().equals(institution.getId()))
                .collect(Collectors.toList());
        userRolesToDelete.forEach(userRole -> this.doUpdateRoleRequest(userRole, OperationType.Remove, userRolesToDelete));

        Map<Application, List<UserRole>> userRolesPerApplication = user.userRolesPerApplicationProvisioningEnabled();
        userRolesPerApplication.entrySet().removeIf(entry -> !entry.getKey().getInstitution().getId().equals(institution.getId()));

        userRolesPerApplication.forEach((application, userRoles) -> {
            Optional<UserRole> userRoleProvisioned = getUserRoleProvisioned(userRoles);
            if (userRoleProvisioned.isPresent()) {
                UserRole userRole = userRoleProvisioned.get();
                String userRequest = prettyJson(new UserRequest(user, userRole));
                this.deleteRequest(application, userRequest, USER_API, userRole);
            }
        });

    }

    @Override
    public void newRoleRequest(Role role) {
        doNewRoleRequest(role, Collections.emptyList());
    }

    @Override
    public void updateRoleRequest(UserRole userRole, OperationType operationType) {
        doUpdateRoleRequest(userRole, operationType, Collections.emptyList());
    }

    @Override
    public void deleteRolesRequest(Role role) {
        if (role.getApplication().provisioningEnabled() && StringUtils.hasText(role.getServiceProviderId())) {
            String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
            String groupRequest = prettyJson(new GroupRequest(externalId, role, role.getName(), Collections.emptyList()));
            this.deleteRequest(role.getApplication(), groupRequest, GROUP_API, role);
        }
    }

    @Override
    public Optional<Serializable> resendScimFailure(SCIMFailure scimFailure) throws JsonProcessingException {
        Map<String, Object> request = StringUtils.hasText(scimFailure.getMessage()) ? objectMapper.readValue(scimFailure.getMessage(), new TypeReference<>() {
        }) : Collections.emptyMap();
        HttpMethod httpMethod = HttpMethod.valueOf(scimFailure.getHttpMethod());
        if (USER_API.equals(scimFailure.getApi())) {
            switch (httpMethod) {
                case POST -> {
                    return changeUserRequest(request, this::newUserRequest);
                }
                case PUT -> {
                    return changeUserRequest(request, this::updateUserRequest);
                }
                case DELETE -> {
                    this.deleteRequest(
                            scimFailure.getApplication(),
                            scimFailure.getMessage(),
                            USER_API,
                            new ServiceProviderIdentifierRef(scimFailure.getServiceProviderId()));
                    return Optional.empty();
                }
                default -> throw new IllegalArgumentException(String.format("Unknown HTTPmethod %s", scimFailure.getHttpMethod()));
            }
        } else if (GROUP_API.equals(scimFailure.getApi())) {
            switch (httpMethod) {
                case POST -> {
                    String roleUrn = (String) request.get("externalId");
                    ExternalID externalID = GroupURN.parseUrnRole(roleUrn);
                    Role role = roleRepository.findByApplication_institution_homeInstitutionIgnoreCaseAndApplication_nameIgnoreCaseAndNameIgnoreCase(
                            externalID.institutionHome(),
                            externalID.applicationName(),
                            externalID.roleName()
                    ).orElseThrow(NotFoundException::new);
                    this.newRoleRequest(role);
                    return Optional.of(role);
                }
                case PUT, PATCH -> {
                    Role role = roleRepository.findByServiceProviderId(scimFailure.getServiceProviderId())
                            .orElseThrow(NotFoundException::new);
                    this.updateRequest(role.getApplication(), scimFailure.getMessage(), GROUP_API, role, httpMethod);
                    return Optional.of(role);
                }
                case DELETE -> {
                    this.deleteRequest(
                            scimFailure.getApplication(),
                            scimFailure.getMessage(),
                            GROUP_API,
                            new ServiceProviderIdentifierRef(scimFailure.getServiceProviderId()));
                    return Optional.empty();
                }
                default -> throw new IllegalArgumentException(String.format("Unknown HTTPmethod %s", scimFailure.getHttpMethod()));
            }

        } else {
            throw new IllegalArgumentException(String.format("Unknown API %s", scimFailure.getApi()));
        }
    }

    private void doNewRoleRequest(Role role, Collection<UserRole> userRolesToBeDeleted) {
        Application application = role.getApplication();
        if (application.provisioningEnabled()) {
            if (hasEmailHook(application)) {
                role.setServiceProviderId(UUID.randomUUID().toString());
            }
            List<UserRole> userRoles = getUserRoles(role, userRolesToBeDeleted);
            String groupRequest = initiateGroupRequest(role, userRoles);
            this.newRequest(application, groupRequest, GROUP_API, role);
        }
    }

    private List<UserRole> getUserRoles(Role role, Collection<UserRole> userRolesToBeDeleted) {
        Set<Long> userRoleIdentifiers = userRolesToBeDeleted.stream()
                .map(UserRole::getId)
                .collect(Collectors.toSet());
        return userRoleRepository
                .findByRoleId(role.getId())
                .stream()
                .filter(userRole -> !userRoleIdentifiers.contains(userRole.getId()))
                .collect(Collectors.toList());
    }

    private void doUpdateRoleRequest(UserRole userRole, OperationType operationType, Collection<UserRole> userRolesToBeDeleted) {
        Role role = userRole.getRole();
        Application application = role.getApplication();
        if (application.provisioningEnabled()) {
            if (StringUtils.hasText(role.getServiceProviderId())) {
                if (application.isUpdateRolePutMethod()) {
                    List<UserRole> userRoles = getUserRoles(role, userRolesToBeDeleted);
                    String groupRequest = initiateGroupRequest(role, userRoles);
                    this.updateRequest(application, groupRequest, GROUP_API, role, HttpMethod.PUT);
                } else {
                    String groupRequest = patchGroupRequest(role, userRole, operationType);
                    this.updateRequest(application, groupRequest, GROUP_API, role, HttpMethod.PATCH);
                }
            } else {
                this.doNewRoleRequest(role, userRolesToBeDeleted);
            }
        }
    }

    private String initiateGroupRequest(Role role, List<UserRole> userRoles) {
        Collection<Member> members = userRoles.stream()
                .map(userRole -> new Member(userRole.getServiceProviderId()))
                .filter(member -> StringUtils.hasText(member.getValue()))
                .collect(Collectors.toMap(
                        Member::getValue,
                        member -> member,
                        (a1, a2) -> a1))
                .values();
        String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
        return prettyJson(new GroupRequest(externalId, role, role.getName(), new ArrayList<>(members)));
    }

    private String patchGroupRequest(Role role, UserRole userRole, OperationType operationType) {
        String externalId = GroupURN.urnFromRole(groupUrnPrefix, role);
        GroupPatchRequest request = new GroupPatchRequest(externalId, role.getServiceProviderId(), new Operation(operationType, userRole));
        return prettyJson(request);
    }

    private Optional<UserRole> getUserRoleProvisioned(List<UserRole> userRoles) {
        return userRoles.stream()
                .filter(userRole -> StringUtils.hasText(userRole.getServiceProviderId()))
                .findFirst();
    }

    private Optional<Serializable> changeUserRequest(Map<String, Object> request, Consumer<User> userConsumer) {
        String externalId = (String) request.get("externalId");
        User user = userRepository.findByEduPersonPrincipalNameIgnoreCase(externalId).orElseThrow(NotFoundException::new);
        userConsumer.accept(user);
        return Optional.of(user);
    }

    @SneakyThrows
    private void newRequest(Application application, String request, String apiType, ServiceProviderIdentifier serviceProviderIdentifier) {
        if (hasEmailHook(application)) {
            mailBox.sendProvisioningMail(String.format("SCIM %s: CREATE", apiType), request, application.getProvisioningHookEmail());
        } else {
            URI uri = this.provisioningUri(application, apiType, Optional.empty());
            RequestEntity<String> requestEntity = new RequestEntity<>(request, httpHeaders(application), HttpMethod.POST, uri);
            Optional<Map<String, Object>> results = doExchange(requestEntity, apiType, serviceProviderIdentifier, mapParameterizedTypeReference, application);
            results.ifPresent(map -> {
                String id = (String) map.get("id");
                serviceProviderIdentifier.setServiceProviderId(id);
            });
        }
    }

    @SneakyThrows
    private void updateRequest(Application application,
                               String request,
                               String apiType,
                               ServiceProviderIdentifier serviceProviderIdentifier,
                               HttpMethod httpMethod) {
        if (hasEmailHook(application)) {
            mailBox.sendProvisioningMail(String.format("SCIM %s: UPDATE", apiType), request, application.getProvisioningHookEmail());
        } else {
            URI uri = this.provisioningUri(application, apiType, Optional.of(serviceProviderIdentifier));
            RequestEntity<String> requestEntity = new RequestEntity<>(request, httpHeaders(application), httpMethod, uri);
            doExchange(requestEntity, apiType, serviceProviderIdentifier, mapParameterizedTypeReference, application);
        }
    }

    @SneakyThrows
    private void deleteRequest(Application application, String request, String apiType, ServiceProviderIdentifier serviceProviderIdentifier) {
        if (hasEmailHook(application)) {
            mailBox.sendProvisioningMail(String.format("SCIM %s: DELETE", apiType), request, application.getProvisioningHookEmail());
        } else {
            URI uri = this.provisioningUri(application, apiType, Optional.of(serviceProviderIdentifier));
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(application.getProvisioningHookUsername(), application.getProvisioningHookPassword());
            RequestEntity<String> requestEntity = new RequestEntity<>(request, headers, HttpMethod.DELETE, uri);
            doExchange(requestEntity, apiType, serviceProviderIdentifier, stringParameterizedTypeReference, application);
        }
    }

    private <T, S> Optional<T> doExchange(RequestEntity<S> requestEntity,
                                          String api,
                                          ServiceProviderIdentifier serviceProviderIdentifier,
                                          ParameterizedTypeReference<T> typeReference,
                                          Application application) {
        try {
            LOG.info(String.format("Send %s SCIM request with %s httpMethod %s and body %s to %s",
                    api,
                    requestEntity.getUrl(),
                    requestEntity.getMethod(),
                    requestEntity.getBody(),
                    application.getName()));
            return Optional.ofNullable(restTemplate.exchange(requestEntity, typeReference).getBody());
        } catch (RestClientException e) {
            LOG.error("Exception in SCIM exchange", e);
            if (ThreadLocalSCIMFailureStrategy.ignoreFailures()) {
                throw e;
            } else {
                S body = requestEntity.getBody();
                SCIMFailure scimFailure = new SCIMFailure(
                        (String) body,
                        api,
                        requestEntity.getMethod().toString(),
                        requestEntity.getUrl().toString(),
                        serviceProviderIdentifier.getServiceProviderId(),
                        application);
                scimFailureRepository.save(scimFailure);
                mailBox.sendScimFailureMail(scimFailure);
                return Optional.empty();
            }

        }
    }

    private boolean hasEmailHook(Application application) {
        return StringUtils.hasText(application.getProvisioningHookEmail());
    }

    private URI provisioningUri(Application application, String objectType, Optional<ServiceProviderIdentifier> spIdentifier) {
        String postFix = spIdentifier.map(role -> "/" + role.getServiceProviderId()).orElse("");
        return URI.create(String.format("%s/%s%s",
                application.getProvisioningHookUrl(),
                objectType,
                postFix));
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
