package projects.smart_grocery.household;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HouseholdMemberService {
    private static final String OWNER_ROLE = "OWNER";
    private static final String MEMBER_ROLE = "MEMBER";

    private final HouseholdMemberRepository householdMemberRepository;

    public HouseholdMember addOwnerMembership(Long householdId, Long userId) {
        HouseholdMember member = HouseholdMember.builder()
                .householdId(householdId)
                .userId(userId)
                .role(OWNER_ROLE)
                .build();
        return householdMemberRepository.save(member);
    }

    public HouseholdMember addMemberMembership(Long householdId, Long userId) {
        HouseholdMember member = HouseholdMember.builder()
                .householdId(householdId)
                .userId(userId)
                .role(MEMBER_ROLE)
                .build();
        return householdMemberRepository.save(member);
    }

    public HouseholdMember getPrimaryMembershipByUserIdOrThrow(Long userId) {
        return householdMemberRepository.findFirstByUserIdOrderByIdDesc(userId)
                .orElseThrow(() -> new IllegalArgumentException("Household membership not found"));
    }

    public Optional<HouseholdMember> findPrimaryMembershipByUserId(Long userId) {
        return householdMemberRepository.findFirstByUserIdOrderByIdDesc(userId);
    }

    public boolean hasMembership(Long householdId, Long userId) {
        return householdMemberRepository.existsByHouseholdIdAndUserId(householdId, userId);
    }

    public java.util.List<HouseholdMember> getByHouseholdId(Long householdId) {
        return householdMemberRepository.findByHouseholdIdOrderByIdAsc(householdId);
    }
}
