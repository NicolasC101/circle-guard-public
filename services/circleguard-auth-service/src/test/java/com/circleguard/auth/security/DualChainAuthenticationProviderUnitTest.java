package com.circleguard.auth.security;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DualChainAuthenticationProviderUnitTest {

    @Test
    void shouldReturnLdapAuthenticationWhenLdapSucceeds() {
        LdapAuthenticationProvider ldapProvider = Mockito.mock(LdapAuthenticationProvider.class);
        DaoAuthenticationProvider localProvider = Mockito.mock(DaoAuthenticationProvider.class);
        DualChainAuthenticationProvider provider = new DualChainAuthenticationProvider(ldapProvider, localProvider);
        Authentication expected = new UsernamePasswordAuthenticationToken("user", "password");

        Mockito.when(ldapProvider.authenticate(Mockito.any())).thenReturn(expected);

        Authentication result = provider.authenticate(new UsernamePasswordAuthenticationToken("user", "password"));

        assertEquals(expected, result);
        Mockito.verify(localProvider, Mockito.never()).authenticate(Mockito.any());
    }

    @Test
    void shouldFallbackToLocalProviderWhenLdapFails() {
        LdapAuthenticationProvider ldapProvider = Mockito.mock(LdapAuthenticationProvider.class);
        DaoAuthenticationProvider localProvider = Mockito.mock(DaoAuthenticationProvider.class);
        DualChainAuthenticationProvider provider = new DualChainAuthenticationProvider(ldapProvider, localProvider);
        Authentication expected = new UsernamePasswordAuthenticationToken(
                "user",
                "password",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        Mockito.when(ldapProvider.authenticate(Mockito.any())).thenThrow(new BadCredentialsException("LDAP down"));
        Mockito.when(localProvider.authenticate(Mockito.any())).thenReturn(expected);

        Authentication result = provider.authenticate(new UsernamePasswordAuthenticationToken("user", "password"));

        assertEquals(expected, result);
        assertTrue(result.isAuthenticated());
    }
}