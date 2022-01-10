package guests.repository;

import guests.domain.SCIMFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SCIMFailureRepository extends JpaRepository<SCIMFailure, Long> {

    List<SCIMFailure> findByApplication_institution_id(Long institutionId);

}
