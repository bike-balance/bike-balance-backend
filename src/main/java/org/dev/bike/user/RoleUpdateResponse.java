package org.dev.bike.user;

public record RoleUpdateResponse(
        Long userId,
        String username,
        String email,
        UserRole role
) {
    public static RoleUpdateResponse from(User user) {
        return new RoleUpdateResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
