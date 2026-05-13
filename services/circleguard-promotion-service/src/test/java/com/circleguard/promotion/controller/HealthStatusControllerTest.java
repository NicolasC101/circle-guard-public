package com.circleguard.promotion.controller;

import com.circleguard.promotion.service.HealthStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HealthStatusControllerTest {

    @Mock
    private HealthStatusService statusService;

    @InjectMocks
    private HealthStatusController controller;

    @Test
    void confirmPositive_ShouldDelegateToUpdateStatus() {
        controller.confirmPositive(Map.of("anonymousId", "user-1"));

        verify(statusService).updateStatus("user-1", "CONFIRMED");
    }

    @Test
    void resolve_ShouldDelegateToResolveStatusWithoutOverride() {
        controller.resolve(Map.of("anonymousId", "user-1"));

        verify(statusService).resolveStatus("user-1", false);
    }
}
