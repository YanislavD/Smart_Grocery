package projects.smart_grocery.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import projects.smart_grocery.auth.dto.AuthResponse;
import projects.smart_grocery.auth.dto.LoginRequest;
import projects.smart_grocery.auth.dto.RegisterRequest;
import projects.smart_grocery.household.Household;
import projects.smart_grocery.household.HouseholdMemberService;
import projects.smart_grocery.household.HouseholdService;
import projects.smart_grocery.security.JwtService;
import projects.smart_grocery.user.User;
import projects.smart_grocery.user.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private HouseholdService householdService;
    @Mock
    private HouseholdMemberService householdMemberService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_createsUserHouseholdAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("test@mail.com", "secret123");
        User savedUser = User.builder().id(7L).email("test@mail.com").passwordHash("hash").build();
        Household household = Household.builder().id(15L).name("Test household").ownerId(7L).build();

        when(userService.existsByEmail("test@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(userService.save(any(User.class))).thenReturn(savedUser);
        when(householdService.createDefaultHouseholdForUser(7L, "test@mail.com")).thenReturn(household);
        when(jwtService.generateToken("test@mail.com")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        verify(householdMemberService).addOwnerMembership(15L, 7L);
    }

    @Test
    void register_throwsWhenEmailExists() {
        when(userService.existsByEmail("taken@mail.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.register(new RegisterRequest("taken@mail.com", "secret123")));
    }

    @Test
    void login_returnsTokenWhenCredentialsAreValid() {
        User user = User.builder().id(1L).email("user@mail.com").passwordHash("hash").build();
        when(userService.findByEmail("user@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.generateToken("user@mail.com")).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("user@mail.com", "secret"));

        assertEquals("jwt-token", response.accessToken());
    }
}
