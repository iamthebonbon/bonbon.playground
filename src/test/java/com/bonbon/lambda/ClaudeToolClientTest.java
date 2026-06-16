package com.bonbon.lambda;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ToolUseBlock;
import com.anthropic.services.blocking.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaudeToolClientTest {

    @Mock private AnthropicClient anthropicClient;
    @Mock private MessageService messageService;
    @Mock private OrderRepository orderRepository;
    @Mock private Message firstMessage;
    @Mock private Message finalMessage;
    @Mock private ContentBlock textContentBlock;
    @Mock private TextBlock textBlock;
    @Mock private ContentBlock toolUseContentBlock;
    @Mock private ToolUseBlock toolUseBlock;

    @Test
    void invokeReturnsDirectTextWhenNoToolUseRequested() throws Exception {
        when(anthropicClient.messages()).thenReturn(messageService);
        when(messageService.create(any(MessageCreateParams.class))).thenReturn(firstMessage);
        when(firstMessage.stopReason()).thenReturn(Optional.of(StopReason.END_TURN));
        when(firstMessage.content()).thenReturn(List.of(textContentBlock));
        when(textContentBlock.isText()).thenReturn(true);
        when(textContentBlock.asText()).thenReturn(textBlock);
        when(textBlock.text()).thenReturn("No order lookup needed.");

        ClaudeToolClient client = new ClaudeToolClient(anthropicClient, orderRepository);
        String result = client.invoke("What is 2 + 2?");

        assertEquals("No order lookup needed.", result);
        verify(orderRepository, never()).getOrder(any());
        verify(messageService, times(1)).create(any(MessageCreateParams.class));
    }

    @Test
    void invokeCallsOrderRepositoryWhenClaudeRequestsToolUse() throws Exception {
        String orderJson = "{\"orderId\":\"ORD-001\",\"status\":\"SHIPPED\"}";
        when(orderRepository.getOrder("ORD-001")).thenReturn(orderJson);

        when(anthropicClient.messages()).thenReturn(messageService);
        when(messageService.create(any(MessageCreateParams.class))).thenReturn(firstMessage, finalMessage);

        // First turn: Claude issues tool_use
        when(firstMessage.stopReason()).thenReturn(Optional.of(StopReason.TOOL_USE));
        when(toolUseContentBlock.isText()).thenReturn(false);
        when(toolUseContentBlock.isToolUse()).thenReturn(true);
        when(toolUseContentBlock.asToolUse()).thenReturn(toolUseBlock);
        when(toolUseBlock.id()).thenReturn("tool_abc");
        when(toolUseBlock.name()).thenReturn("get_order");
        when(toolUseBlock._input()).thenReturn(JsonValue.from(Map.of("order_id", "ORD-001")));
        when(firstMessage.content()).thenReturn(List.of(toolUseContentBlock));

        // Second turn: Claude returns final text
        when(finalMessage.content()).thenReturn(List.of(textContentBlock));
        when(textContentBlock.isText()).thenReturn(true);
        when(textContentBlock.asText()).thenReturn(textBlock);
        when(textBlock.text()).thenReturn("Order ORD-001 is SHIPPED.");

        ClaudeToolClient client = new ClaudeToolClient(anthropicClient, orderRepository);
        String result = client.invoke("What is the status of order ORD-001?");

        assertEquals("Order ORD-001 is SHIPPED.", result);
        verify(orderRepository).getOrder("ORD-001");
        verify(messageService, times(2)).create(any(MessageCreateParams.class));
    }

    @Test
    void invokeThrowsUpstreamExceptionOnApiError() {
        when(anthropicClient.messages()).thenReturn(messageService);
        when(messageService.create(any(MessageCreateParams.class))).thenThrow(new RuntimeException("network failure"));

        ClaudeToolClient client = new ClaudeToolClient(anthropicClient, orderRepository);

        assertThrows(ClaudeClient.UpstreamException.class, () -> client.invoke("check order ORD-999"));
    }
}
