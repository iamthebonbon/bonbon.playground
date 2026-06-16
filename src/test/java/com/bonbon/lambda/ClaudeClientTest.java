package com.bonbon.lambda;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.services.blocking.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaudeClientTest {

    @Mock
    private AnthropicClient anthropicClient;
    @Mock
    private MessageService messageService;
    @Mock
    private Message message;
    @Mock
    private ContentBlock contentBlock;
    @Mock
    private TextBlock textBlock;

    @Test
    void invokeExtractsFirstTextContentBlock() throws Exception {
        when(anthropicClient.messages()).thenReturn(messageService);
        when(messageService.create(any(MessageCreateParams.class))).thenReturn(message);
        when(message.content()).thenReturn(List.of(contentBlock));
        when(contentBlock.isText()).thenReturn(true);
        when(contentBlock.asText()).thenReturn(textBlock);
        when(textBlock.text()).thenReturn("Hello!");

        ClaudeClient client = new ClaudeClient(anthropicClient);
        String result = client.invoke("Say hello");

        assertEquals("Hello!", result);

        ArgumentCaptor<MessageCreateParams> paramsCaptor = ArgumentCaptor.forClass(MessageCreateParams.class);
        verify(messageService).create(paramsCaptor.capture());
        assertNotNull(paramsCaptor.getValue());
    }

    @Test
    void invokeThrowsUpstreamExceptionOnSdkError() throws Exception {
        when(anthropicClient.messages()).thenReturn(messageService);
        when(messageService.create(any(MessageCreateParams.class))).thenThrow(new RuntimeException("network failure"));

        ClaudeClient client = new ClaudeClient(anthropicClient);

        assertThrows(ClaudeClient.UpstreamException.class, () -> client.invoke("test"));
    }
}
