## Why

`System.out.println` in `Handler.java` and `ClaudeClient.java` produces unstructured output that is hard to filter in CloudWatch Logs and carries no log level, making it impossible to distinguish errors from debug noise.

## What Changes

- Add a `java.util.logging.Logger` field to `Handler` and `ClaudeClient`
- Replace all `System.out.println(...)` calls with appropriate `logger.warning(...)` or `logger.severe(...)` calls

## Capabilities

### New Capabilities
- `structured-logging`: Lambda-compatible structured logging via `java.util.logging`, replacing raw stdout prints

### Modified Capabilities
<!-- none -->

## Impact

- `src/main/java/com/bonbon/lambda/Handler.java` — catch blocks
- `src/main/java/com/bonbon/lambda/ClaudeClient.java` — all three catch blocks
- No new dependencies; `java.util.logging` is part of the JDK
