package projects.smart_grocery.pantry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HouseholdMemberService {

    private final HouseholdMemberRepository householdMemberRepository;

    public HouseholdMember addOwnerMembership(Long householdId, Long userId) {
        HouseholdMember member = HouseholdMember.builder()
                .householdId(householdId)
                .userId(userId)
                .role("OWNER")
                .build();
        return householdMemberRepository.save(member);
    }

    public HouseholdMember getPrimaryMembershipByUserIdOrThrow(Long userId) {
        return householdMemberRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Household membership not found"));
    }

    public Optional<HouseholdMember> findPrimaryMembershipByUserId(Long userId) {
        return householdMemberRepository.findFirstByUserId(userId);
    }
}
