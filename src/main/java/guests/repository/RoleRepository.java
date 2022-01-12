package guests.repository;

import guests.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByApplication_institution_id(Long institutionId);

    List<Role> findByApplication_id(Long applicationId);

    Optional<Role> findByApplication_idAndNameIgnoreCase(Long applicationId, String name);

    Optional<Role> findByApplication_institution_homeInstitutionIgnoreCaseAndApplication_nameIgnoreCaseAndNameIgnoreCase(
            String institutionHomeLowerCase, String applicationNameLowerCase, String roleNameLowerCase);

    Optional<Role> findByServiceProviderId(String serviceProviderId);

}
