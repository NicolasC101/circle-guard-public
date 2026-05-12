package com.circleguard.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Component
public class IdentityClient {
    private final RestTemplate restTemplate;
    private final String identityUrl;

    public IdentityClient(RestTemplate restTemplate,
                          @Value("${circleguard.identity-service.url:http://localhost:8083/api/v1/identities/map}") String identityUrl) {
        this.restTemplate = restTemplate;
        this.identityUrl = identityUrl;
    }

    public UUID getAnonymousId(String realIdentity) {
        Map<String, String> request = Map.of("realIdentity", realIdentity);
        Map response = restTemplate.postForObject(identityUrl, request, Map.class);
        return UUID.fromString(response.get("anonymousId").toString());
    }
}
