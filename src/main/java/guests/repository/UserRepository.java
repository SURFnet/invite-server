package guests.repository;

import guests.domain.Application;
import guests.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEduPersonPrincipalNameIgnoreCase(String eduPersonPrincipalName);

    Optional<User> findByUnspecifiedIdIgnoreCase(String unspecifiedId);

    List<User> findByInstitution_id(Long institutionId);

    List<User> findByRoles_role_application_id(Long applicationId);
}
