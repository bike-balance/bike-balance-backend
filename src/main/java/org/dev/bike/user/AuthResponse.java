package org.dev.bike.user;

public record AuthResponse(
        Long userId,
        String username,
        String email,
        UserRole role,
        String token
) {

    public static AuthResponse from(User user, String token) {
        return new AuthResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                token
        );
    }
}
