package projects.smart_grocery.household;

import java.time.Instant;

public record IncomingHouseholdInviteView(
        String householdName,
        String token,
        Instant createdAt
) {
}
