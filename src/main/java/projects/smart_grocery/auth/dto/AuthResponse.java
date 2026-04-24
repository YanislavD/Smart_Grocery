package projects.smart_grocery.auth.dto;

public record AuthResponse(

        String accessToken,
        String tokenType
) {}
