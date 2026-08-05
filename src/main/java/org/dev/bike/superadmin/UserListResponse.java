package org.dev.bike.superadmin;

import org.dev.bike.user.User;
import org.dev.bike.user.UserRole;

public record UserListResponse(
        Long userId,
        String username,
        String email,
        UserRole role
) {
    public static UserListResponse from(User user) {
        return new UserListResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
