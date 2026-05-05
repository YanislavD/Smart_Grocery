package projects.smart_grocery.household;

public record HouseholdMemberView(
        Long userId,
        String email,
        String role
) {
}
