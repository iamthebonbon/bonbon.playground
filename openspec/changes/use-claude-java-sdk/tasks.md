## 1. Update Dependencies

- [x] 1.1 Add `com.anthropic:anthropic-java` to `pom.xml` (latest stable version); remove the `java.net.http.HttpClient`-based Claude call path (keep the SSM dependency — it is unaffected)

## 2. Rewrite ClaudeClient

- [x] 2.1 Replace the `HttpClient httpClient` field and package-private constructor with an `AnthropicClient anthropicClient` field; update the public constructor to build `AnthropicOkHttpClient` with the cached `API_KEY`; add a package-private constructor that accepts an injected `AnthropicClient` for tests
- [x] 2.2 Rewrite `invoke(String prompt)`: build `MessageCreateParams` with model `claude-haiku-4-5-20251001`, `maxTokens(1024)`, and the user prompt; call `anthropicClient.messages().create(params)`; extract the first text content block from the response; map SDK exceptions (`AnthropicException` and subclasses) to `UpstreamException`
- [x] 2.3 Remove the now-unused imports (`java.net.http.*`, `com.fasterxml.jackson` nodes used only for request building) — keep Jackson only if still needed for response parsing (it won't be)

## 3. Update Tests

- [x] 3.1 Rewrite `ClaudeClientTest`: mock `AnthropicClient` and its `messages()` chain; stub `create()` to return a `Message` with a single text content block; assert the extracted text matches; verify the correct model and prompt are passed in `MessageCreateParams`
- [ ] 3.2 Verify `HandlerTest` still compiles and passes unchanged (no `Handler.java` edits expected)

## 4. Build & Smoke Test

- [ ] 4.1 Run `mvn clean package` — confirm fat JAR builds without errors
- [ ] 4.2 Run `sam local invoke ClaudeInvokeFunction -e event.json --env-vars env.json` — confirm HTTP 200 with a non-empty `response` field
