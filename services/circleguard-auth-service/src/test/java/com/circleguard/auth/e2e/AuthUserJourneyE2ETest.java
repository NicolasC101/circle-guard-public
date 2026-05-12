package com.circleguard.auth.e2e;

import com.circleguard.auth.AuthServiceApplication;
import com.circleguard.auth.client.IdentityClient;
import com.circleguard.auth.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.Key;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath; // Corrected import
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=my-super-secret-test-key-32-chars-long",
        "jwt.expiration=3600000",
        "qr.secret=my-qr-secret-key-for-dev-1234567890",
        "qr.expiration=600000",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.allow-bean-definition-overriding=true"
})
class AuthUserJourneyE2ETest {

    private static final String IDENTITY_URL = "http://localhost:8083/api/v1/identities/map";
    private static final String JWT_SECRET = "my-super-secret-test-key-32-chars-long";
    private static final String QR_SECRET = "my-qr-secret-key-for-dev-1234567890";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

        private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private AuthenticationManager authManager;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void shouldAuthenticateLocalUserAndReturnJwt() throws Exception {
        UUID anonymousId = UUID.fromString("550e8400-e29b-41d4-a716-446655440210");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "maria",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        org.mockito.Mockito.when(authManager.authenticate(org.mockito.ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        mockServer.expect(requestTo(IDENTITY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"realIdentity\":\"maria\"}"))
                .andRespond(withSuccess("{\"anonymousId\":\"" + anonymousId + "\"}", MediaType.APPLICATION_JSON));

        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"maria\"," +
                                "\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anonymousId").value(anonymousId.toString()))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(responseBody);
        String issuedToken = responseJson.get("token").asText();
        Key key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(issuedToken).getBody();

        assertEquals(anonymousId.toString(), claims.getSubject());
        assertEquals(List.of("ROLE_ADMIN"), claims.get("permissions", List.class));
        mockServer.verify();
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        org.mockito.Mockito.when(authManager.authenticate(org.mockito.ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
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
    void shouldReturnInternalServerErrorWhenIdentityVaultFails() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "maria",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        org.mockito.Mockito.when(authManager.authenticate(org.mockito.ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
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

    @Test
    void shouldGenerateVisitorHandoffPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/visitor/handoff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"anonymousId\":\"550e8400-e29b-41d4-a716-446655440211\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.handoffPayload").value(org.hamcrest.Matchers.startsWith("HANDOFF_TOKEN:")));
    }

    @Test
    void shouldGenerateQrTokenForAuthenticatedAnonymousUser() throws Exception {
        String qrResponseBody = mockMvc.perform(get("/api/v1/auth/qr/generate")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("550e8400-e29b-41d4-a716-446655440212"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value("60"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String issuedToken = objectMapper.readTree(qrResponseBody).get("qrToken").asText();
        Key key = Keys.hmacShaKeyFor(QR_SECRET.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(issuedToken).getBody();

        assertEquals("550e8400-e29b-41d4-a716-446655440212", claims.getSubject());
    }
}