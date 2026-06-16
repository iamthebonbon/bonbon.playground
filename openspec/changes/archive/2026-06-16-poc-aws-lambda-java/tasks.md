## 1. Maven Project Setup

- [x] 1.1 Create `pom.xml` with `aws-lambda-java-core`, `aws-lambda-java-events`, `jackson-databind`, and `aws-sdk-java-v2` SSM client; configure `maven-shade-plugin` to produce a fat JAR
- [x] 1.2 Create directory structure: `src/main/java/com/bonbon/lambda/` and `src/test/java/com/bonbon/lambda/`
- [x] 1.3 Update `.gitignore` to exclude `target/` and SAM build artifacts (`.aws-sam/`)

## 2. Lambda Handler

- [x] 2.1 Implement `Handler.java` implementing `RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>` — parse JSON body, extract `prompt`, delegate to `ClaudeClient`, return `{"response": "..."}` or error JSON with appropriate status codes
- [x] 2.2 Add input validation: return HTTP 400 with `{"error": "prompt is required"}` when `prompt` is null or blank

## 3. Claude API Client

- [x] 3.1 Implement `ClaudeClient.java` using Java 17 `HttpClient` — build the `POST /v1/messages` request with correct headers (`x-api-key`, `anthropic-version`, `content-type`) and `claude-haiku-4-5-20251001` as the model
- [x] 3.2 Implement SSM key retrieval in `ClaudeClient`: read `/bonbon/anthropic-api-key` as a `SecureString` from SSM at construction time and cache it in a `static final` field

## 4. SAM Template & Deployment Config

- [x] 4.1 Create `template.yaml` (SAM): define `ClaudeInvokeFunction` with Java 17 runtime, 512 MB memory, 30 s timeout, `Handler::handleRequest` as handler, and `ssm:GetParameter` in the IAM policy
- [x] 4.2 Create `event.json` test fixture: `{"version":"2.0","requestContext":{"http":{"method":"POST"}},"body":"{\"prompt\":\"Say hello\"}"}`

## 5. Unit Tests

- [x] 5.1 Add JUnit 5 + Mockito to `pom.xml` test scope; write `HandlerTest` covering: valid prompt returns 200, missing prompt returns 400
- [x] 5.2 Write `ClaudeClientTest` with a mocked `HttpClient` verifying correct request headers and URL, and that the first text content block is extracted from the response

## 6. Local Smoke Test

- [x] 6.1 Run `mvn clean package` — confirm the fat JAR is produced under `target/`
- [x] 6.2 Run `sam local invoke ClaudeInvokeFunction -e event.json` — confirm HTTP 200 and a non-empty `response` field in the output (requires `ANTHROPIC_API_KEY` set locally or SSM emulation)
