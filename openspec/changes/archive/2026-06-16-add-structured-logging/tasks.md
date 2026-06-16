## 1. ClaudeClient — Add Logger

- [x] 1.1 Add `private static final Logger LOG = Logger.getLogger(ClaudeClient.class.getName())` field and `import java.util.logging.Logger`
- [x] 1.2 Replace `System.out.println(e.getMessage())` in the `UpstreamException` catch block with `LOG.warning(e.getMessage())`
- [x] 1.3 Replace `System.out.println(e.getMessage())` in the `AnthropicException` catch block with `LOG.warning(e.getMessage())`
- [x] 1.4 Replace `System.out.println(e.getMessage())` in the generic `Exception` catch block with `LOG.severe(e.getMessage())`

## 2. Handler — Add Logger

- [x] 2.1 Add `private static final Logger LOG = Logger.getLogger(Handler.class.getName())` field and `import java.util.logging.Logger`
- [x] 2.2 Replace any `System.out.println` calls in `handleRequest` catch blocks with appropriate `LOG.warning` or `LOG.severe` calls

## 3. Verify

- [x] 3.1 Run `mvn test` — all existing tests pass (no behaviour change expected)
