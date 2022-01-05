package guests.repository;

import guests.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    @EntityGraph(value = "findByHash", type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {"inviter.institutionMemberships.institution"})
    Optional<Invitation> findByHashAndStatus(String hash, Status status);

    List<Invitation> findByInstitution_id(Long institutionId);

    List<Invitation> findByRoles_role_application_id(Long applicationId);
}
