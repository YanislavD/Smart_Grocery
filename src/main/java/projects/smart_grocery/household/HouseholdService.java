package projects.smart_grocery.household;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HouseholdService {

    private final HouseholdRepository householdRepository;

    public Household getByIdOrThrow(Long householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));
    }

    public Household createDefaultHouseholdForUser(Long userId, String email) {
        String householdName = email.contains("@")
                ? email.substring(0, email.indexOf("@")) + "'s household"
                : "My household";

        Household household = Household.builder()
                .name(householdName)
                .ownerId(userId)
                .build();

        return householdRepository.save(household);
    }

    public Household renameHousehold(Long householdId, String newName) {
        String normalized = newName == null ? "" : newName.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Household name is required");
        }
        Household household = getByIdOrThrow(householdId);
        household.setName(normalized);
        return householdRepository.save(household);
    }
}
