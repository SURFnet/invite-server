package guests.repository;

import guests.domain.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    List<Institution> findByInstitutionMemberships_user_id(Long userId);

    Optional<Institution> findByHomeInstitutionIgnoreCase(String homeInstitution);

    Optional<Institution> findByEntityIdIgnoreCase(String entityId);

    @Query(
            value = "select count(*) from users u inner join user_roles ur on ur.user_id = u.id " +
                    "inner join roles r on r.id = ur.role_id inner join applications a on a.id = r.application_id " +
                    "inner join institutions i on a.institution_id = i.id where i.id = ?1",
            nativeQuery = true
    )
    Long countUsers(Long institutionId);

}
