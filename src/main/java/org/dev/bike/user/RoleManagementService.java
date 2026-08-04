package org.dev.bike.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleManagementService {

    private final UserRepository userRepository;

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
