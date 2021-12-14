package guests.repository;

import guests.domain.Application;
import guests.domain.Institution;
import guests.domain.Role;
import guests.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByApplication_institution(Institution institution);

    Optional<Role> findByApplication_idAndNameIgnoreCase(Long applicationId, String name);

}
