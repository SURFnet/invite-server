package guests.scim;

import com.fasterxml.jackson.core.JsonProcessingException;
import guests.domain.*;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.Optional;

public interface SCIMService {

    void newUserRequest(User user);

    void updateUserRequest(User user);

    void deleteUserRequest(User user);

    void deleteUserByInstitutionRequest(User user, Institution institution);

    void newRoleRequest(Role role);

    void updateRoleRequest(UserRole userRole, OperationType operationType);

    void deleteRolesRequest(Role role);

    Optional<Serializable> resendScimFailure(SCIMFailure scimFailure) throws JsonProcessingException;
}
