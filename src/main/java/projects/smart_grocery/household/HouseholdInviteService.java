package projects.smart_grocery.household;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projects.smart_grocery.user.User;
import projects.smart_grocery.user.UserService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HouseholdInviteService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final HouseholdInviteRepository householdInviteRepository;
    private final HouseholdMemberService householdMemberService;
    private final UserService userService;
    private final HouseholdService householdService;

    public HouseholdInvite createInvite(Long householdId, Long invitedByUserId, String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (email.isBlank()) {
            throw new IllegalArgumentException("Invite email is required");
        }

        User invitedBy = userService.getByIdOrThrow(invitedByUserId);
        if (invitedBy.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("You cannot invite yourself");
        }

        userService.findByEmail(email).ifPresent(user -> {
            if (householdMemberService.hasMembership(householdId, user.getId())) {
                throw new IllegalArgumentException("This user is already in your household");
            }
        });

        householdInviteRepository.findFirstByHouseholdIdAndEmailAndStatus(householdId, email, STATUS_PENDING)
                .ifPresent(invite -> {
                    throw new IllegalArgumentException("A pending invite already exists for this email");
                });

        HouseholdInvite invite = HouseholdInvite.builder()
                .householdId(householdId)
                .email(email)
                .token(UUID.randomUUID().toString().replace("-", ""))
                .status(STATUS_PENDING)
                .invitedByUserId(invitedByUserId)
                .build();

        return householdInviteRepository.save(invite);
    }

    public void acceptInvite(String token, Long currentUserId) {
        HouseholdInvite invite = householdInviteRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        if (!STATUS_PENDING.equals(invite.getStatus())) {
            throw new IllegalArgumentException("Invite is no longer active");
        }

        User user = userService.getByIdOrThrow(currentUserId);
        if (!user.getEmail().equalsIgnoreCase(invite.getEmail())) {
            throw new IllegalArgumentException("This invite was sent to a different email");
        }

        if (!householdMemberService.hasMembership(invite.getHouseholdId(), user.getId())) {
            householdMemberService.addMemberMembership(invite.getHouseholdId(), user.getId());
        }

        invite.setStatus(STATUS_ACCEPTED);
        invite.setAcceptedByUserId(user.getId());
        invite.setAcceptedAt(Instant.now());
        householdInviteRepository.save(invite);
    }

    public List<HouseholdInviteView> getInvitesForHousehold(Long householdId) {
        return householdInviteRepository.findByHouseholdIdOrderByCreatedAtDesc(householdId)
                .stream()
                .map(invite -> new HouseholdInviteView(
                        invite.getId(),
                        invite.getEmail(),
                        invite.getStatus(),
                        invite.getToken(),
                        invite.getCreatedAt()
                ))
                .toList();
    }

    public List<IncomingHouseholdInviteView> getIncomingPendingInvitesForEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return householdInviteRepository.findByEmailAndStatusOrderByCreatedAtDesc(normalizedEmail, STATUS_PENDING)
                .stream()
                .map(invite -> new IncomingHouseholdInviteView(
                        householdService.getByIdOrThrow(invite.getHouseholdId()).getName(),
                        invite.getToken(),
                        invite.getCreatedAt()
                ))
                .toList();
    }

    public void cancelInvite(Long householdId, Long inviteId) {
        HouseholdInvite invite = householdInviteRepository.findByIdAndHouseholdId(inviteId, householdId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        if (!STATUS_PENDING.equals(invite.getStatus())) {
            throw new IllegalArgumentException("Only pending invites can be cancelled");
        }
        invite.setStatus(STATUS_CANCELLED);
        householdInviteRepository.save(invite);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.toLowerCase().trim();
    }
}
