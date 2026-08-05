package org.dev.bike.superadmin;

import org.dev.bike.user.User;
import org.dev.bike.user.UserRepository;
import org.dev.bike.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleManagementService roleManagementService;

    @Test
    void returnsUsersInPagesOfTen() {
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(15L);
        when(user.getUsername()).thenReturn("tester");
        when(user.getEmail()).thenReturn("tester@example.com");
        when(user.getRole()).thenReturn(UserRole.USER);
        when(userRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(1, 10), 21));

        UserPageResponse response = roleManagementService.getUsers(1);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(21);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.users()).singleElement().satisfies(item -> {
            assertThat(item.userId()).isEqualTo(15L);
            assertThat(item.role()).isEqualTo(UserRole.USER);
        });

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("userId").isDescending()).isTrue();
    }
}
