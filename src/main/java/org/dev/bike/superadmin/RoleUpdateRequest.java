package org.dev.bike.superadmin;

import jakarta.validation.constraints.NotNull;
import org.dev.bike.user.UserRole;

public record RoleUpdateRequest(
        @NotNull UserRole role
) {
}
