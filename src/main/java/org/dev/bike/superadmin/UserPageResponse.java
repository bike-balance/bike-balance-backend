package org.dev.bike.superadmin;

import org.dev.bike.user.User;
import org.springframework.data.domain.Page;

import java.util.List;

public record UserPageResponse(
        List<UserListResponse> users,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static UserPageResponse from(Page<User> userPage) {
        return new UserPageResponse(
                userPage.getContent().stream().map(UserListResponse::from).toList(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isFirst(),
                userPage.isLast()
        );
    }
}
