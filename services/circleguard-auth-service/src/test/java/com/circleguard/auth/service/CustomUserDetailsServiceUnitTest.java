package com.circleguard.auth.service;

import com.circleguard.auth.model.LocalUser;
import com.circleguard.auth.model.Permission;
import com.circleguard.auth.model.Role;
import com.circleguard.auth.repository.LocalUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceUnitTest {

    @Mock
    private LocalUserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void shouldMapRolesAndPermissionsForActiveUser() {
        Permission permission = Permission.builder().name("READ_CAMPUS").build();
        Role role = Role.builder().name("ADMIN").permissions(Set.of(permission)).build();
        LocalUser user = LocalUser.builder()
                .username("maria")
                .password("hashed-password")
                .isActive(true)
                .roles(Set.of(role))
                .build();

        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("maria");

        assertEquals("maria", details.getUsername());
        assertEquals("hashed-password", details.getPassword());
        assertEquals(2, details.getAuthorities().size());
    }

    @Test
    void shouldRejectInactiveUser() {
        LocalUser user = LocalUser.builder()
                .username("juan").password("hashed-password")
                .isActive(false)
                .roles(Set.of())
                .build();

        when(userRepository.findByUsername("juan")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> service.loadUserByUsername("juan"));
    }
}