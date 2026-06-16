## Why

`ClaudeClient.java` currently builds HTTP requests manually using Java's built-in `HttpClient`, hand-crafting JSON payloads and parsing responses. The official Anthropic Java SDK provides typed request/response models, built-in retries, and proper error handling — eliminating this boilerplate and keeping the client in sync with API changes automatically.

## What Changes

- Replace the manual `HttpClient` + Jackson JSON construction in `ClaudeClient.java` with the official `anthropic-java` SDK client
- Add `com.anthropic:anthropic-java` to `pom.xml`; remove the now-redundant direct use of `java.net.http.HttpClient` for Claude API calls
- Update `ClaudeClientTest.java` to mock SDK types instead of raw `HttpClient`

## Capabilities

### New Capabilities

_(none — same external behavior, implementation only)_

### Modified Capabilities

_(none — the `claude-invoke` requirements are unchanged; only the internal HTTP mechanism changes)_

## Impact

- **Modified files**: `pom.xml`, `src/main/java/com/bonbon/lambda/ClaudeClient.java`, `src/test/java/com/bonbon/lambda/ClaudeClientTest.java`
- **Dependencies added**: `com.anthropic:anthropic-java`
- **No breaking changes** — request/response contract with API Gateway is identical
- **SSM key retrieval unchanged** — `loadApiKey()` logic stays as-is
