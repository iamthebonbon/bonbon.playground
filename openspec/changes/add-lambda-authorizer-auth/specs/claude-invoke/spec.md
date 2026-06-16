## MODIFIED Requirements

### Requirement: Accept prompt via HTTP POST
The system SHALL accept an HTTP POST request to the Lambda endpoint with a JSON body containing a `prompt` field (string). Requests with a missing or empty `prompt` field SHALL be rejected with HTTP 400. Requests lacking a valid `x-api-key` header SHALL be rejected with HTTP 403 by the API Gateway Lambda Authorizer before reaching the handler.

#### Scenario: Valid prompt submitted with valid API key
- **WHEN** a client sends `POST /` with body `{"prompt": "Hello"}` and a valid `x-api-key` header
- **THEN** the Lambda returns HTTP 200 with `Content-Type: application/json`

#### Scenario: Missing prompt field
- **WHEN** a client sends `POST /` with body `{}` or an empty body (and a valid `x-api-key`)
- **THEN** the Lambda returns HTTP 400 with body `{"error": "prompt is required"}`

#### Scenario: Missing or invalid API key
- **WHEN** a client sends `POST /` without an `x-api-key` header or with an incorrect value
- **THEN** API Gateway returns HTTP 403 and the handler Lambda is never invoked
