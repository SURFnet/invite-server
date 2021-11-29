package guests.repository;

import guests.domain.Application;
import guests.domain.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByHomeInstitutionIgnoreCase(String homeInstitution);

}
