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
class ToolHandlerTest {

    @Mock
    private ClaudeToolClient toolClient;

    @Test
    void validPromptReturns200() throws Exception {
        when(toolClient.invoke("What is order ORD-1?")).thenReturn("Order ORD-1 is SHIPPED.");

        ToolHandler handler = new ToolHandler(toolClient);
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder()
                .withBody("{\"prompt\":\"What is order ORD-1?\"}")
                .build();

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Order ORD-1 is SHIPPED."));
    }

    @Test
    void missingPromptReturns400() {
        ToolHandler handler = new ToolHandler(toolClient);
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder()
                .withBody("{}")
                .build();

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("prompt is required"));
    }

    @Test
    void upstreamErrorReturns502() throws Exception {
        when(toolClient.invoke(any())).thenThrow(new ClaudeClient.UpstreamException("api down"));

        ToolHandler handler = new ToolHandler(toolClient);
        APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder()
                .withBody("{\"prompt\":\"check order ORD-2\"}")
                .build();

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);

        assertEquals(502, response.getStatusCode());
        assertTrue(response.getBody().contains("upstream error"));
    }
}
