## ADDED Requirements

### Requirement: Validate API key from request header
The authorizer Lambda SHALL read the `x-api-key` header from the incoming HTTP API v2 request and compare it to the value stored in SSM Parameter Store (`/bonbon/api-key`, SecureString). If the values match, the authorizer SHALL return `{"isAuthorized": true}`; otherwise it SHALL return `{"isAuthorized": false}`.

#### Scenario: Valid API key provided
- **WHEN** a request arrives with `x-api-key: <correct-secret>`
- **THEN** the authorizer returns `{"isAuthorized": true}` and API Gateway forwards the request to the handler

#### Scenario: Invalid API key provided
- **WHEN** a request arrives with `x-api-key: <wrong-value>`
- **THEN** the authorizer returns `{"isAuthorized": false}` and API Gateway returns HTTP 403 to the caller

#### Scenario: Missing x-api-key header
- **WHEN** a request arrives with no `x-api-key` header
- **THEN** the authorizer returns `{"isAuthorized": false}` and API Gateway returns HTTP 403 to the caller

### Requirement: Cache SSM secret at cold start
The authorizer Lambda SHALL read `/bonbon/api-key` from SSM Parameter Store once at cold start and cache it in a static field for the lifetime of the container.

#### Scenario: Key cached after first invocation
- **WHEN** the authorizer Lambda container is warm
- **THEN** no SSM API call is made for subsequent authorizer invocations

### Requirement: API Gateway caches authorizer result
API Gateway SHALL be configured with a 300-second authorizer result cache keyed on the `x-api-key` header value. This eliminates repeated authorizer Lambda invocations for the same valid key within the cache window.

#### Scenario: Repeated requests with same valid key
- **WHEN** a caller sends multiple requests with the same valid `x-api-key` within 300 seconds
- **THEN** only the first request triggers an authorizer Lambda invocation; subsequent requests are allowed by the cached result
