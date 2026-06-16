package com.bonbon.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.logging.Logger;

public class ToolHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Logger LOG = Logger.getLogger(ToolHandler.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ClaudeToolClient toolClient;

    public ToolHandler() {
        this.toolClient = new ClaudeToolClient();
    }

    ToolHandler(ClaudeToolClient toolClient) {
        this.toolClient = toolClient;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            String body = event.getBody();
            if (body == null || body.isBlank()) {
                return errorResponse(400, "prompt is required");
            }

            JsonNode json = MAPPER.readTree(body);
            JsonNode promptNode = json.get("prompt");
            if (promptNode == null || promptNode.asText().isBlank()) {
                return errorResponse(400, "prompt is required");
            }

            String responseText = toolClient.invoke(promptNode.asText());

            ObjectNode result = MAPPER.createObjectNode();
            result.put("response", responseText);

            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(MAPPER.writeValueAsString(result))
                    .build();

        } catch (ClaudeClient.UpstreamException e) {
            LOG.warning(e.getMessage());
            return errorResponse(502, "upstream error");
        } catch (Exception e) {
            LOG.severe(e.getMessage());
            return errorResponse(500, "internal error");
        }
    }

    private APIGatewayV2HTTPResponse errorResponse(int statusCode, String message) {
        try {
            ObjectNode error = MAPPER.createObjectNode();
            error.put("error", message);
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(statusCode)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(MAPPER.writeValueAsString(error))
                    .build();
        } catch (Exception e) {
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(statusCode)
                    .withBody("{\"error\":\"" + message + "\"}")
                    .build();
        }
    }
}
