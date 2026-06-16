## Context

`Handler.java` and `ClaudeClient.java` use `System.out.println` in catch blocks. On Lambda, stdout lands in CloudWatch Logs with no level metadata, making it impossible to filter errors from informational output or set up metric filters.

## Goals / Non-Goals

**Goals:**
- Replace `System.out.println` with `java.util.logging.Logger` in both classes
- Use `WARNING` for caught-and-rethrown exceptions, `SEVERE` for unexpected errors

**Non-Goals:**
- Adding SLF4J, Logback, or Log4j (unnecessary deps for a PoC; JUL ships with Java 17)
- Changing log output format or routing (Lambda's default JUL handler already writes to CloudWatch)
- Structured JSON logging (out of scope for PoC)

## Decisions

**`java.util.logging` (JUL) over SLF4J/Logback**
Zero new dependencies. Lambda's runtime already configures a `ConsoleHandler` that writes to stdout → CloudWatch. JUL levels (`WARNING`, `SEVERE`) map cleanly to the two severity tiers needed here.

**Per-class static logger**
```java
private static final Logger LOG = Logger.getLogger(Handler.class.getName());
```
Standard JUL pattern. Static avoids allocating a logger per Lambda invocation.

**Log level mapping**
| Situation | Level |
|-----------|-------|
| `UpstreamException` re-thrown | `WARNING` |
| `AnthropicException` mapped to `UpstreamException` | `WARNING` |
| Unexpected `Exception` | `SEVERE` |
| Missing/blank prompt in `Handler` | `WARNING` |

## Risks / Trade-offs

- JUL default format includes timestamp and class name but not JSON → acceptable for PoC
- If structured JSON logging is needed later, drop in `aws-lambda-java-log4j2` or a JUL formatter; logger field stays the same
