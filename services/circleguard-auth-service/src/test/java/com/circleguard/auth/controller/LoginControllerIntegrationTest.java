package com.circleguard.auth.controller;

import com.circleguard.auth.client.IdentityClient;
import com.circleguard.auth.config.AuthHttpClientConfig;
import com.circleguard.auth.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "jwt.secret=my-super-secret-test-key-32-chars-long",
        "jwt.expiration=3600000"
})
@org.springframework.context.annotation.Import({AuthHttpClientConfig.class, IdentityClient.class})
class LoginControllerIntegrationTest {

    private static final String IDENTITY_URL = "http://localhost:8083/api/v1/identities/map";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @MockBean
    private AuthenticationManager authManager;

    @MockBean
    private JwtTokenService jwtService;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUpMockServer() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void shouldReturnBearerTokenWhenCredentialsAreValid() throws Exception {
        UUID anonymousId = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "maria",
                "password",
                List.of(() -> "ROLE_ADMIN")
        );

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        mockServer.expect(requestTo(IDENTITY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"realIdentity\":\"maria\"}"))
                .andRespond(withSuccess("{\"anonymousId\":\"" + anonymousId + "\"}", MediaType.APPLICATION_JSON));
        when(jwtService.generateToken(any(UUID.class), any(Authentication.class))).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"maria\"," +
                                "\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.anonymousId").value(anonymousId.toString()))
                .andExpect(jsonPath("$.type").value("Bearer"));

        mockServer.verify();
    }

    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("invalid"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"maria\"," +
                                "\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void shouldReturnServerErrorWhenIdentityServiceFails() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "maria",
                "password",
                List.of(() -> "ROLE_ADMIN")
        );

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        mockServer.expect(requestTo(IDENTITY_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"maria\"," +
                                "\"password\":\"password123\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Internal server error")));
    }
}