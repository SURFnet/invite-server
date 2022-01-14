package guests.repository;

import guests.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByEndDateBefore(Instant instant);

    List<UserRole> findByRoleId(Long roleId);


}
