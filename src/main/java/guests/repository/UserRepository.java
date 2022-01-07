package guests.repository;

import guests.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(value = "findByEduPersonPrincipalNameIgnoreCase",
            type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {"roles.role.application"})
    Optional<User> findByEduPersonPrincipalNameIgnoreCase(String eduPersonPrincipalName);

    Optional<User> findByUnspecifiedIdIgnoreCase(String unspecifiedId);

    List<User> findByInstitutionMemberships_Institution_id(Long institutionId);

    List<User> findByRoles_role_application_id(Long applicationId);

    List<User> findByRoles_role_id(Long roleId);

    List<User> findByLastActivityBefore(Instant instant);
}
