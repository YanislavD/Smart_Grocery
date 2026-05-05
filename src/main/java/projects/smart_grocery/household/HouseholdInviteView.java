package projects.smart_grocery.household;

import java.time.Instant;

public record HouseholdInviteView(
        Long id,
        String email,
        String status,
        String token,
        Instant createdAt
) {
}
