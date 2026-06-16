## Requirements

### Requirement: Lambda classes use JUL logger instead of stdout
`Handler` and `ClaudeClient` SHALL declare a `private static final java.util.logging.Logger` field and use it instead of `System.out.println` for all diagnostic output.

#### Scenario: Error in ClaudeClient is logged at WARNING
- **WHEN** `ClaudeClient.invoke()` catches an `AnthropicException` or `UpstreamException`
- **THEN** the exception message is logged at `WARNING` level before the exception is re-thrown or wrapped

#### Scenario: Unexpected exception is logged at SEVERE
- **WHEN** `ClaudeClient.invoke()` catches a non-Anthropic `Exception`
- **THEN** the exception message is logged at `SEVERE` level before wrapping in `UpstreamException`

#### Scenario: Handler logs upstream errors
- **WHEN** `Handler.handleRequest()` catches a `ClaudeClient.UpstreamException`
- **THEN** the exception message is logged at `WARNING` level before returning a 502 response
