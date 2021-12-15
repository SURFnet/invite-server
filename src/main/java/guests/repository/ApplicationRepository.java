package guests.repository;

import guests.domain.Application;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByEntityIdIgnoreCase(String entityId);

    List<Application> findByRoles_IdIn(List roleIdentifiers);

    List<Application> findByInstitution_id(long institutionId);

}
