package org.dev.bike.superadmin;

import lombok.RequiredArgsConstructor;
import org.dev.bike.user.User;
import org.dev.bike.user.UserRepository;
import org.dev.bike.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleManagementService {

    private static final int PAGE_SIZE = 10;

    private final UserRepository userRepository;

    public UserPageResponse getUsers(int page) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater");
        }

        Page<User> users = userRepository.findAll(
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "userId"))
        );

        return UserPageResponse.from(users);
    }

    @Transactional
    public RoleUpdateResponse updateRole(Long actorUserId, Long targetUserId, UserRole requestedRole) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

        if (actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can change roles");
        }

        if (actorUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own role");
        }

        if (requestedRole != UserRole.USER && requestedRole != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only USER or ADMIN can be assigned through the API");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (target.getRole() == UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPER_ADMIN cannot be changed through the API");
        }

        target.changeRole(requestedRole);
        return RoleUpdateResponse.from(target);
    }
}
