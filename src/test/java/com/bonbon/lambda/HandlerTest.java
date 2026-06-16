package com.bonbon.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandlerTest {

    @Mock
    private ClaudeClient claudeClient;

    @Test
    void validPromptReturns200() throws Exception {
        when(claudeClient.invoke("Hello")).thenReturn("Hi there");

        Handler handler = new Handler(claudeClient);
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder()
                .withBody("{\"prompt\":\"Hello\"}")
                .build();

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Hi there"));
    }

    @Test
    void missingPromptReturns400() {
        Handler handler = new Handler(claudeClient);
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder()
                .withBody("{}")
                .build();

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("prompt is required"));
    }
}
