package projects.smart_grocery.household;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, Long> {
    Optional<HouseholdMember> findFirstByUserIdOrderByIdDesc(Long userId);
    List<HouseholdMember> findByHouseholdIdOrderByIdAsc(Long householdId);
    boolean existsByHouseholdIdAndUserId(Long householdId, Long userId);
}
