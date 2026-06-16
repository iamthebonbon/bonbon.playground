## Context

Greenfield project. No existing code. Goal is a minimal, deployable Lambda that accepts a user prompt via HTTP POST and returns a Claude API response. Tech stack is constrained to AWS Lambda + Java 17. Cost and operational simplicity are primary concerns.

## Goals / Non-Goals

**Goals:**
- Deployable Lambda handler wired to API Gateway HTTP API
- Integration with Claude API using Java's built-in `HttpClient` (no extra HTTP lib)
- SAM-based local testing (`sam local invoke`) and deployment (`sam deploy`)
- API key stored securely in SSM Parameter Store (not hardcoded)
- Unit-testable handler and client code

**Non-Goals:**
- Production-grade auth or rate limiting
- Multi-environment CI/CD pipeline
- GraalVM native image (adds build complexity, not needed for PoC)
- Streaming Claude responses
- Persistent conversation history

## Decisions

**Build tool: Maven**
Standard for AWS Lambda Java. SAM builds (`sam build`) have native Maven support. Gradle is equally viable but Maven is more commonly seen in Lambda tutorials and avoids Gradle version friction.

**HTTP client: Java 17 built-in `HttpClient`**
Avoids adding OkHttp, Apache HttpClient, or similar. The built-in client supports synchronous blocking calls, which is sufficient for a Lambda (one request at a time per instance).

**Claude model: `claude-haiku-4-5-20251001`**
Lowest cost per token. Fastest response. Appropriate for a PoC that needs to demonstrate the integration, not maximize output quality.

**API Gateway: HTTP API (v2)**
Lower cost and lower latency than REST API (v1). Uses `APIGatewayV2HTTPEvent` and `APIGatewayV2HTTPResponse` in the handler.

**API key storage: SSM Parameter Store (SecureString)**
Lambda reads `ANTHROPIC_API_KEY` from SSM at cold start and caches it in a static field. Avoids plaintext env vars. SAM template grants `ssm:GetParameter` permission to the Lambda execution role.

**Lambda memory: 512 MB, timeout: 30 s**
512 MB balances cold-start JVM time against cost. 30 s gives headroom for Claude API latency without hitting API Gateway's 29 s limit — adjust if needed.

**SnapStart: disabled for PoC**
SnapStart reduces cold starts but adds complexity (snapshot lifecycle hooks). Acceptable to skip for a proof-of-concept.

## Risks / Trade-offs

- **JVM cold start (~1–2 s)** → Acceptable for PoC; can add SnapStart later if cold start is a concern
- **SSM read adds ~50–200 ms at cold start** → Cached after first read, negligible for subsequent invocations
- **No input validation beyond null check** → Sufficient for PoC; add schema validation before production use
- **Claude API errors surface as 502** → Caller gets a structured error body; sufficient for PoC debugging
