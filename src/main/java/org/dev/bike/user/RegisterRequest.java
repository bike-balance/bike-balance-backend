package org.dev.bike.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 50)
        String username,

        @NotBlank
        @Size(min = 2, max = 255)
        String password,

        @NotBlank
        @Email
        @Size(max = 100)
        String email
) {
}
