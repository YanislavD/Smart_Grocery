package projects.smart_grocery.pantry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, Long> {
    Optional<HouseholdMember> findFirstByUserId(Long userId);
}
