## Context

`ClaudeClient.java` currently calls the Claude API by manually building a JSON body with Jackson, sending it via Java's built-in `HttpClient`, and parsing the response by hand. The official Anthropic Java SDK (`com.anthropic:anthropic-java`) provides typed request/response models and handles serialization, error classification, and retry semantics internally — removing all of that manual work.

The rest of the system (`Handler.java`, SAM template, SSM key retrieval) is unaffected.

## Goals / Non-Goals

**Goals:**
- Replace the manual HTTP + JSON layer in `ClaudeClient.java` with the official SDK
- Keep `ClaudeClient`'s public API identical (`invoke(String prompt) throws UpstreamException`) so `Handler.java` needs no changes
- Update `ClaudeClientTest.java` to mock SDK types

**Non-Goals:**
- Changing `Handler.java`, `template.yaml`, or SSM key retrieval logic
- Adding streaming, tool use, or other SDK features not currently used
- Upgrading other dependencies

## Decisions

**SDK dependency: `com.anthropic:anthropic-java`**
The official Anthropic Java SDK. Provides `AnthropicOkHttpClient` (sync blocking) which is appropriate for Lambda (one request at a time per instance). No extra HTTP library needed — the SDK bundles OkHttp.

**API key injection: constructor parameter**
The SDK client requires an API key at construction time. `ClaudeClient` already resolves the key via `loadApiKey()` into a `static final` field — pass it to the SDK client builder:
```java
AnthropicOkHttpClient.builder().apiKey(API_KEY).build()
```
The SDK client instance is stored as an instance field (same pattern as the current `HttpClient` field).

**Testability: inject `AnthropicClient` via package-private constructor**
`AnthropicClient` is an interface in the SDK — Mockito can mock it directly. The package-private constructor accepts an `AnthropicClient` for tests; the public constructor builds a real `AnthropicOkHttpClient`. This matches the existing pattern.

**Error mapping: SDK exceptions → `UpstreamException`**
The SDK throws `AnthropicException` subtypes (e.g., `AnthropicServiceException`) for non-2xx responses. Catch these and rethrow as `UpstreamException` to keep `Handler.java` unchanged.

**Response extraction**
SDK response: `MessageResponse` → `.content()` → first `ContentBlock` with `type() == ContentBlockType.TEXT` → `.text().get()`. Equivalent to the current `content[0].text` JSON path.

## Risks / Trade-offs

- **OkHttp on Lambda cold start** → OkHttp adds ~1–2 MB to the fat JAR and a small cold-start overhead vs. the built-in `HttpClient`. Acceptable for a PoC; native `HttpClient` can be restored later if cold start becomes a concern.
- **SDK version drift** → Pinning a specific SDK version in `pom.xml` avoids surprise API breaks during iterative PoC work.
