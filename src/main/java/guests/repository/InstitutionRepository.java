package guests.repository;

import guests.domain.Application;
import guests.domain.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    List<Institution> findByInstitutionMemberships_user_id(Long userId);

    Optional<Institution> findByHomeInstitutionIgnoreCase(String homeInstitution);

    Optional<Institution> findByEntityIdIgnoreCase(String entityId);
}
