# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**bonbon.playground** is an AWS Lambda proof-of-concept service that exposes the Anthropic Claude API via HTTP. It demonstrates:
- Java 17 Lambda functions with AWS SAM (Serverless Application Model)
- API Gateway integration with custom authorization
- Secure credential management via AWS Systems Manager Parameter Store
- Claude API invocation using the Anthropic Java SDK

The service accepts POST requests with a prompt, invokes Claude Haiku, and returns the response.

## Common Development Commands

### Build & Packaging
```bash
mvn clean compile          # Compile sources only
mvn clean package          # Compile and create uber-JAR (used for Lambda deployment)
mvn clean install          # Package and install to local Maven repo
```

### Testing
```bash
mvn test                   # Run all JUnit 5 tests with test API key injected
mvn test -Dtest=HandlerTest -DfailIfNoTests=false  # Run single test class
```

### Local Development
```bash
sam build                  # Build Lambda artifacts
sam local invoke ClaudeInvokeFunction -e event.json  # Test with sample event locally
sam validate               # Validate template.yaml syntax
```

### AWS Deployment
```bash
sam deploy                 # Deploy to eu-north-1 (configured in samconfig.toml)
sam delete                 # Remove CloudFormation stack
```

## Architecture

### Three-Layer Design

**1. Authorization Layer** (`Authorizer.java`)
- Validates `x-api-key` header on all API requests
- Retrieves API key from AWS SSM Parameter Store (`/bonbon/api-key`)
- Caches key in Lambda container for cold-start optimization
- Returns 401 on invalid key

**2. Request Handler** (`Handler.java`)
- Implements AWS Lambda `RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>`
- Parses incoming JSON: `{"prompt": "string"}`
- Delegates to `ClaudeClient` for API invocation
- Returns JSON: `{"response": "string"}` or `{"error": "string"}`
- Handles HTTP error codes: 400 (bad request), 502 (upstream error), 500 (internal error)

**3. Claude Client** (`ClaudeClient.java`)
- Wraps Anthropic Java SDK (v2.40.1)
- Model: `claude-haiku-4-5-20251001` (Haiku 4.5)
- Max tokens: 1024
- Credential resolution (in order):
  1. System property: `anthropic.api.key` (tests use this)
  2. Environment variable: `ANTHROPIC_API_KEY`
  3. AWS SSM Parameter Store: `/bonbon/anthropic-api-key` (production)
- Throws custom `UpstreamException` for API failures
- Caches API key at Lambda cold start

### Infrastructure (AWS SAM)

- **HTTP API**: Serverless HTTP API with `x-api-key` authorization
- **AuthorizerFunction**: Custom Lambda authorizer
- **ClaudeInvokeFunction**: Main handler (invoked by API)
- Both functions have IAM permissions to read SSM parameters
- Deployment region: `eu-north-1`

## Key Implementation Patterns

### Error Handling
- Validation errors return 400 with descriptive messages
- API failures return 502 (upstream error)
- Unexpected exceptions return 500 with error detail

### Testing Strategy
- Unit tests mock AWS SDK and Anthropic client using Mockito
- Test API key injected via Maven Surefire system property
- `HandlerTest.java`: HTTP layer validation
- `ClaudeClientTest.java`: Claude SDK integration testing

### Maven Build Configuration
- **maven-shade-plugin**: Creates fat JAR with all dependencies for Lambda deployment
- **maven-surefire-plugin**: Runs tests with test API key injection
- **maven-compiler-plugin**: Java 17 source/target with UTF-8 encoding

### AWS SAM Template Structure
- `template.yaml`: Defines Lambda functions, environment variables, and IAM policies
- `samconfig.toml`: Deployment defaults (stack name, region)
- `event.json`: Sample Lambda event for local testing

## Important Notes

### Lambda Container Optimization
- API keys are cached in the Lambda container to avoid repeated SSM Parameter Store calls
- Cold starts initialize once; warm invocations reuse cached credentials

### Anthropic API Integration
- Requires valid `ANTHROPIC_API_KEY` in SSM Parameter Store or environment
- Uses `AnthropicOkHttpClient` for HTTP communication
- Respects Anthropic SDK rate limits and retry logic

### AWS Credentials
- Deployment requires AWS credentials configured locally (or in CI/CD)
- SAM automatically creates Lambda execution role with SSM read permissions
- Authorizer validates API keys from SSM Parameter Store

## Verification & Testing

### Unit Tests
```bash
mvn test
```
Validates:
- HTTP request/response handling
- API key validation
- Claude API invocation
- Error scenarios

### Local Integration Test
```bash
sam build
sam local invoke ClaudeInvokeFunction -e event.json
```
Simulates Lambda execution with sample event: `{"prompt": "Hello, Claude"}`

### Pre-Deployment Checklist
1. `mvn clean package` succeeds
2. `mvn test` passes
3. `sam validate` succeeds
4. Valid API key in SSM Parameter Store: `/bonbon/api-key` and `/bonbon/anthropic-api-key`
5. AWS credentials configured for `eu-north-1`

## Directory Structure

```
src/main/java/com/bonbon/lambda/
├── Handler.java           # HTTP request handler
├── ClaudeClient.java      # Claude API client
└── Authorizer.java        # API key authorization

src/test/java/com/bonbon/lambda/
├── HandlerTest.java       # Handler unit tests
└── ClaudeClientTest.java  # ClaudeClient unit tests

template.yaml              # AWS SAM infrastructure
samconfig.toml             # Deployment configuration
event.json                 # Local test event
pom.xml                    # Maven build configuration
```

## Development Workflow

1. **Make changes** to source files in `src/main/java/`
2. **Run tests** with `mvn test` to validate
3. **Build locally** with `sam build && sam local invoke`
4. **Deploy** with `sam deploy` (or via CI/CD pipeline)

## OpenSpec Integration

This project uses OpenSpec for structured change management. Active and archived changes are stored in the `openspec/` directory. Use OpenSpec skills in Claude Code to manage project specifications and changes.

## Instructions
- Keep cost and memory efficient solutions
- Keep operational efficiency
- Don't propose complexity
- Be extremely concise
- Don't overhead
