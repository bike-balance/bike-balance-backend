package org.dev.bike.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dev.bike.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super-admin/users")
@RequiredArgsConstructor
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    @PatchMapping("/{userId}/role")
    public ResponseEntity<RoleUpdateResponse> updateRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody RoleUpdateRequest request
    ) {
        return ResponseEntity.ok(
                roleManagementService.updateRole(principal.getUserId(), userId, request.role())
        );
    }
}
