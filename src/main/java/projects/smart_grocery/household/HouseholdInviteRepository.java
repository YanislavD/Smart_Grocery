package projects.smart_grocery.household;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HouseholdInviteRepository extends JpaRepository<HouseholdInvite, Long> {
    Optional<HouseholdInvite> findByToken(String token);
    Optional<HouseholdInvite> findByIdAndHouseholdId(Long id, Long householdId);
    Optional<HouseholdInvite> findFirstByHouseholdIdAndEmailAndStatus(Long householdId, String email, String status);
    List<HouseholdInvite> findByHouseholdIdOrderByCreatedAtDesc(Long householdId);
    List<HouseholdInvite> findByEmailAndStatusOrderByCreatedAtDesc(String email, String status);
}
