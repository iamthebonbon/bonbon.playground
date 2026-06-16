package com.bonbon.lambda;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.anthropic.models.messages.ToolUseBlockParam;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Draft PoC: Claude client with tool use for DynamoDB order lookup.
 *
 * Flow:
 *   1. Send user prompt with get_order tool definition
 *   2. If Claude issues tool_use, execute it against DynamoDB
 *   3. Send the tool result back; return Claude's final text answer
 */
public class ClaudeToolClient {

    private static final Logger LOG = Logger.getLogger(ClaudeToolClient.class.getName());

    static final String API_KEY = loadApiKey();

    private final AnthropicClient anthropicClient;
    private final OrderRepository orderRepository;
    private final Tool getOrderTool;

    public ClaudeToolClient() {
        this.anthropicClient = AnthropicOkHttpClient.builder().apiKey(API_KEY).build();
        this.orderRepository = new OrderRepository();
        this.getOrderTool = buildGetOrderTool();
    }

    ClaudeToolClient(AnthropicClient anthropicClient, OrderRepository orderRepository) {
        this.anthropicClient = anthropicClient;
        this.orderRepository = orderRepository;
        this.getOrderTool = buildGetOrderTool();
    }

    private static Tool buildGetOrderTool() {
        return Tool.builder()
                .name("get_order")
                .description("Look up order information by order ID from the database.")
                .inputSchema(Tool.InputSchema.builder()
                        .type(JsonValue.from("object"))
                        .properties(JsonValue.from(Map.of(
                                "order_id", Map.of(
                                        "type", "string",
                                        "description", "The unique order ID to look up"
                                )
                        )))
                        .required(JsonValue.from(List.of("order_id")))
                        .build())
                .build();
    }

    public String invoke(String prompt) throws ClaudeClient.UpstreamException {
        try {
            // Turn 1: send prompt with tool definition
            MessageCreateParams firstTurn = MessageCreateParams.builder()
                    .model(Model.CLAUDE_HAIKU_4_5_20251001)
                    .maxTokens(1024L)
                    .addTool(getOrderTool)
                    .addUserMessage(prompt)
                    .build();

            Message firstResponse = anthropicClient.messages().create(firstTurn);

            if (!StopReason.TOOL_USE.equals(firstResponse.stopReason().orElse(null))) {
                return extractText(firstResponse);
            }

            // Claude requested a tool — extract it
            ToolUseBlock toolUse = firstResponse.content().stream()
                    .filter(ContentBlock::isToolUse)
                    .findFirst()
                    .map(ContentBlock::asToolUse)
                    .orElseThrow(() -> new ClaudeClient.UpstreamException("Missing tool_use block"));

            String toolResult = executeTool(toolUse.name(), toolUse._input());
            LOG.info("Tool '" + toolUse.name() + "' returned: " + toolResult);

            // Rebuild assistant content blocks for conversation history
            List<ContentBlockParam> assistantContent = new ArrayList<>();
            for (ContentBlock block : firstResponse.content()) {
                if (block.isText()) {
                    assistantContent.add(ContentBlockParam.ofText(
                            TextBlockParam.builder().text(block.asText().text()).build()));
                } else if (block.isToolUse()) {
                    ToolUseBlock tu = block.asToolUse();
                    assistantContent.add(ContentBlockParam.ofToolUse(
                            ToolUseBlockParam.builder()
                                    .id(tu.id())
                                    .name(tu.name())
                                    .input(tu._input())
                                    .build()));
                }
            }

            // Turn 2: send tool result and get final answer
            MessageCreateParams secondTurn = MessageCreateParams.builder()
                    .model(Model.CLAUDE_HAIKU_4_5_20251001)
                    .maxTokens(1024L)
                    .addTool(getOrderTool)
                    .addUserMessage(prompt)
                    .addAssistantMessageOfBlockParams(assistantContent)
                    .addUserMessageOfBlockParams(List.of(
                            ContentBlockParam.ofToolResult(
                                    ToolResultBlockParam.builder()
                                            .toolUseId(toolUse.id())
                                            .content(toolResult)
                                            .build()
                            )
                    ))
                    .build();

            Message finalResponse = anthropicClient.messages().create(secondTurn);
            return extractText(finalResponse);

        } catch (ClaudeClient.UpstreamException e) {
            throw e;
        } catch (Exception e) {
            LOG.severe("Tool invoke error: " + e.getMessage());
            throw new ClaudeClient.UpstreamException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String executeTool(String name, JsonValue input) throws ClaudeClient.UpstreamException {
        try {
            if ("get_order".equals(name)) {
                Optional<Object> objOpt = input.asObject();
                if (!objOpt.isPresent()) {
                    throw new ClaudeClient.UpstreamException("Tool input is not an object");
                }
                Map<String, JsonValue> obj = (Map<String, JsonValue>) objOpt.get();
                JsonValue orderIdValue = obj.get("order_id");
                Optional<Object> strOpt = orderIdValue != null ? orderIdValue.asString() : Optional.empty();
                if (!strOpt.isPresent()) {
                    throw new ClaudeClient.UpstreamException("Missing or invalid order_id");
                }
                return orderRepository.getOrder((String) strOpt.get());
            }
            throw new ClaudeClient.UpstreamException("Unknown tool: " + name);
        } catch (ClaudeClient.UpstreamException e) {
            throw e;
        } catch (Exception e) {
            throw new ClaudeClient.UpstreamException("Tool execution failed: " + e.getMessage());
        }
    }

    private String extractText(Message message) throws ClaudeClient.UpstreamException {
        return message.content().stream()
                .filter(ContentBlock::isText)
                .findFirst()
                .map(b -> b.asText().text())
                .orElseThrow(() -> new ClaudeClient.UpstreamException("No text content in response"));
    }

    private static String loadApiKey() {
        String prop = System.getProperty("anthropic.api.key");
        if (prop != null && !prop.isBlank()) return prop;
        String env = System.getenv("ANTHROPIC_API_KEY");
        if (env != null && !env.isBlank()) return env;
        try (SsmClient ssm = SsmClient.create()) {
            return ssm.getParameter(r -> r.name("/bonbon/anthropic-api-key").withDecryption(true))
                    .parameter().value();
        }
    }
}
