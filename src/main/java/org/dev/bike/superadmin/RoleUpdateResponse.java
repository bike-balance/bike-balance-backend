package org.dev.bike.superadmin;

import org.dev.bike.user.User;
import org.dev.bike.user.UserRole;

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
