package com.circleguard.auth.controller;

import com.circleguard.auth.service.QrTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QrTokenController.class)
@AutoConfigureMockMvc(addFilters = false)
class QrTokenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrTokenService qrTokenService;

    @Test
    void shouldGenerateQrTokenForAuthenticatedUser() throws Exception {
        when(qrTokenService.generateQrToken(any(UUID.class))).thenReturn("mock-qr-token");

        mockMvc.perform(get("/api/v1/auth/qr/generate")
                .principal(new UsernamePasswordAuthenticationToken("550e8400-e29b-41d4-a716-446655440020", "N/A"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrToken").value("mock-qr-token"))
                .andExpect(jsonPath("$.expiresIn").value("60"));
    }
}