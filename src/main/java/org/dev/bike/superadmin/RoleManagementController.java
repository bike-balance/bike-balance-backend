package org.dev.bike.superadmin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dev.bike.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super-admin/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    @GetMapping
    public ResponseEntity<UserPageResponse> getUsers(
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(roleManagementService.getUsers(page));
    }

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
