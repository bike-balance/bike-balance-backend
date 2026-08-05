package org.dev.bike.user;

public record CurrentUserResponse(
        Long userId,
        String username,
        String email,
        UserRole role
) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
