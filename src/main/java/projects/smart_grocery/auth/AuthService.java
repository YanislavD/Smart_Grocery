package projects.smart_grocery.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import projects.smart_grocery.auth.dto.AuthResponse;
import projects.smart_grocery.auth.dto.LoginRequest;
import projects.smart_grocery.auth.dto.RegisterRequest;
import projects.smart_grocery.pantry.Household;
import projects.smart_grocery.pantry.HouseholdMemberService;
import projects.smart_grocery.pantry.HouseholdService;
import projects.smart_grocery.security.JwtService;
import projects.smart_grocery.user.User;
import projects.smart_grocery.user.UserService;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final HouseholdService householdService;
    private final HouseholdMemberService householdMemberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        if (userService.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        User createdUser = userService.save(user);
        Household household = householdService.createDefaultHouseholdForUser(createdUser.getId(), createdUser.getEmail());
        householdMemberService.addOwnerMembership(household.getId(), createdUser.getId());

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer");
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        User user = userService.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        boolean ok = passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!ok) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer");
    }
}