## Why

There is no working Java AWS Lambda application in this repository. We need a minimal, deployable proof-of-concept that demonstrates invoking the Claude API from a Java 17 Lambda — establishing a baseline we can iterate on.

## What Changes

- Add a Maven project with Lambda handler wired to an API Gateway HTTP event
- Add a thin Claude API HTTP client using Java's built-in `HttpClient`
- Add an AWS SAM template (`template.yaml`) for local testing and deployment
- Add JUnit 5 unit tests for handler and client
- Update `.gitignore` to exclude build artifacts

## Capabilities

### New Capabilities

- `claude-invoke`: Accept an HTTP POST request with a user prompt, call the Claude API (claude-haiku-4-5-20251001 for cost efficiency), and return the model's text response as JSON.

### Modified Capabilities

_(none — greenfield project)_

## Impact

- **New files**: `pom.xml`, `src/`, `template.yaml`, `event.json` (test fixture)
- **Dependencies added**: `aws-lambda-java-core`, `aws-lambda-java-events`, `jackson-databind` (JSON serialization)
- **No breaking changes** (no existing code)
- **AWS services**: Lambda (512 MB, 30s timeout), API Gateway (HTTP API), SSM Parameter Store for `ANTHROPIC_API_KEY`
