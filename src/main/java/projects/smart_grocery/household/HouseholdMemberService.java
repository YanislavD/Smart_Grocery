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

    public HouseholdMember getByHouseholdAndUserOrThrow(Long householdId, Long userId) {
        return householdMemberRepository.findByHouseholdIdAndUserId(householdId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Household member not found"));
    }

    public boolean isOwner(Long householdId, Long userId) {
        return householdMemberRepository.findByHouseholdIdAndUserId(householdId, userId)
                .map(member -> OWNER_ROLE.equals(member.getRole()))
                .orElse(false);
    }

    public long ownerCount(Long householdId) {
        return householdMemberRepository.countByHouseholdIdAndRole(householdId, OWNER_ROLE);
    }

    public void changeRole(Long householdId, Long targetUserId, String newRole) {
        String normalizedRole = newRole == null ? "" : newRole.trim().toUpperCase();
        if (!OWNER_ROLE.equals(normalizedRole) && !MEMBER_ROLE.equals(normalizedRole)) {
            throw new IllegalArgumentException("Invalid role");
        }
        HouseholdMember target = getByHouseholdAndUserOrThrow(householdId, targetUserId);
        if (OWNER_ROLE.equals(target.getRole()) && MEMBER_ROLE.equals(normalizedRole) && ownerCount(householdId) <= 1) {
            throw new IllegalArgumentException("At least one owner must remain");
        }
        target.setRole(normalizedRole);
        householdMemberRepository.save(target);
    }

    public void removeMember(Long householdId, Long targetUserId) {
        HouseholdMember target = getByHouseholdAndUserOrThrow(householdId, targetUserId);
        if (OWNER_ROLE.equals(target.getRole()) && ownerCount(householdId) <= 1) {
            throw new IllegalArgumentException("Cannot remove the last owner");
        }
        householdMemberRepository.delete(target);
    }
}
