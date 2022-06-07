package guests.repository;

import guests.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(value = "findByEduPersonPrincipalNameIgnoreCase",
            type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {"userRoles.role.application"})
    Optional<User> findByEduPersonPrincipalNameIgnoreCase(String eduPersonPrincipalName);

    Optional<User> findBySubIgnoreCase(String sub);

    List<User> findByInstitutionMemberships_Institution_id(Long institutionId);

    List<User> findByUserRoles_role_application_id(Long applicationId);

    List<User> findByLastActivityBefore(Instant instant);

    @Query(value = "select u.email, u.given_name, u.family_name from users u inner join institution_memberships m on m.user_id = u.id where m.institution_id = ?1 and m.authority = 'GUEST'",
            nativeQuery = true)
    List<Map<String, String>> findEmailAndNameByInstitution_id(Long institutionId);
}
