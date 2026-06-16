package com.bonbon.lambda;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.logging.Logger;

public class ClaudeClient {

    private static final Logger LOG = Logger.getLogger(ClaudeClient.class.getName());

    // Fetched at cold start and cached for the lifetime of the Lambda instance
    static final String API_KEY = loadApiKey();

    private final AnthropicClient anthropicClient;

    public ClaudeClient() {
        this.anthropicClient = AnthropicOkHttpClient.builder()
                .apiKey(API_KEY)
                .build();
    }

    ClaudeClient(AnthropicClient anthropicClient) {
        this.anthropicClient = anthropicClient;
    }

    private static String loadApiKey() {
        String sysProp = System.getProperty("anthropic.api.key");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp;
        }
        String envVar = System.getenv("ANTHROPIC_API_KEY");
        if (envVar != null && !envVar.isBlank()) {
            return envVar;
        }
        try (SsmClient ssm = SsmClient.create()) {
            return ssm.getParameter(r -> r.name("/bonbon/anthropic-api-key").withDecryption(true))
                    .parameter().value();
        }
    }

    public String invoke(String prompt) throws UpstreamException {
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(Model.CLAUDE_HAIKU_4_5_20251001)
                    .maxTokens(1024L)
                    .addUserMessage(prompt)
                    .build();

            var message = anthropicClient.messages().create(params);

            return message.content().stream()
                    .filter(com.anthropic.models.messages.ContentBlock::isText)
                    .findFirst()
                    .map(block -> block.asText().text())
                    .orElseThrow(() -> new UpstreamException("No text content in response"));

        } catch (UpstreamException e) {
            LOG.warning(e.getMessage());
            throw e;
        } catch (AnthropicException e) {
            LOG.warning(e.getMessage());
            throw new UpstreamException(e.getMessage());
        } catch (Exception e) {
            LOG.severe(e.getMessage());
            throw new UpstreamException(e.getMessage());
        }
    }

    public static class UpstreamException extends Exception {
        public UpstreamException(String message) {
            super(message);
        }
    }
}
