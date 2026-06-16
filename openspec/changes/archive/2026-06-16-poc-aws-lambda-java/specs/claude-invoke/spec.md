## ADDED Requirements

### Requirement: Accept prompt via HTTP POST
The system SHALL accept an HTTP POST request to the Lambda endpoint with a JSON body containing a `prompt` field (string). Requests with a missing or empty `prompt` field SHALL be rejected with HTTP 400.

#### Scenario: Valid prompt submitted
- **WHEN** a client sends `POST /` with body `{"prompt": "Hello"}`
- **THEN** the Lambda returns HTTP 200 with `Content-Type: application/json`

#### Scenario: Missing prompt field
- **WHEN** a client sends `POST /` with body `{}` or an empty body
- **THEN** the Lambda returns HTTP 400 with body `{"error": "prompt is required"}`

### Requirement: Call Claude API with user prompt
The system SHALL forward the user's prompt to the Claude API (`claude-haiku-4-5-20251001`) using the Messages API (`POST /v1/messages`). The API key SHALL be retrieved from AWS SSM Parameter Store and cached for the lifetime of the Lambda instance.

#### Scenario: Successful Claude API call
- **WHEN** the prompt is non-empty and the Claude API is reachable
- **THEN** the Lambda calls `POST https://api.anthropic.com/v1/messages` with the correct headers and returns the first text content block from the response

#### Scenario: Claude API unreachable or returns error
- **WHEN** the Claude API returns a non-2xx response or times out
- **THEN** the Lambda returns HTTP 502 with body `{"error": "upstream error"}`

### Requirement: Return Claude response as JSON
The system SHALL return the Claude API's text response in a JSON envelope: `{"response": "<text>"}` with HTTP 200.

#### Scenario: Response format
- **WHEN** Claude API returns a successful message
- **THEN** the Lambda response body is `{"response": "<claude text output>"}` and status is 200

### Requirement: Secure API key management
The system SHALL read the Anthropic API key from AWS SSM Parameter Store (`/bonbon/anthropic-api-key`) as a `SecureString`. The key SHALL NOT appear in environment variables, logs, or source code.

#### Scenario: Key loaded at cold start
- **WHEN** the Lambda container starts for the first time
- **THEN** the key is fetched from SSM, decrypted, and stored in a static field for reuse

#### Scenario: Key missing or access denied
- **WHEN** the SSM parameter does not exist or the Lambda role lacks permission
- **THEN** the Lambda fails to initialize and returns HTTP 500 for all requests until redeployed with correct configuration
