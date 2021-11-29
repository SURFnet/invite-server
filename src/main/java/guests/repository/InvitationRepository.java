package guests.repository;

import guests.domain.Application;
import guests.domain.Invitation;
import guests.domain.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    @EntityGraph(value = "findByHash", type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"inviter.institution"})
    Optional<Invitation> findByHash(String hash);

}
