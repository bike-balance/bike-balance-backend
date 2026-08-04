package org.dev.bike.security;

import org.dev.bike.user.User;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return user.getUserId();
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return switch (user.getRole()) {
            case USER -> List.of(new SimpleGrantedAuthority("ROLE_USER"));
            case ADMIN -> List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
            case SUPER_ADMIN -> List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")
            );
        };
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    @NonNull
    public String getUsername() {
        return user.getEmail();
    }
}
