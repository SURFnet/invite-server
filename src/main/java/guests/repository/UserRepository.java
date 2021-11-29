package guests.repository;

import guests.domain.Application;
import guests.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEduPersonPrincipalNameIgnoreCase(String eduPersonPrincipalName);

}
