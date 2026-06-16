## Context

The `POST /` route on the HTTP API Gateway is unauthenticated. Any caller with the URL can invoke the Lambda and trigger Anthropic API charges. A Lambda Authorizer intercepts requests before they reach the handler, validates a shared API key from the `x-api-key` header, and returns an allow/deny decision to API Gateway.

Current request path:
```
Caller → API GW → ClaudeInvokeFunction
```

After change:
```
Caller → API GW → AuthorizerFunction (validates x-api-key) → ClaudeInvokeFunction (if allowed)
```

## Goals / Non-Goals

**Goals:**
- Reject unauthenticated requests at the API Gateway layer (before Lambda invocation)
- Zero changes to `Handler.java` or `ClaudeClient.java`
- Secret stored in SSM Parameter Store (SecureString), never in code or env vars
- Cache authorizer result to minimize SSM calls and cold-start penalty

**Non-Goals:**
- Per-user identity or fine-grained permissions
- Token expiry or key rotation workflow
- Multiple API keys / usage tracking

## Decisions

**Payload format: v2.0 (simple response)**
HTTP API v2 authorizers support a simple `{"isAuthorized": true/false}` response. No IAM policy document needed. Simpler and sufficient for allow/deny.

**Authorizer result caching: 300s TTL**
API Gateway caches the authorizer response keyed on the `x-api-key` header value. Avoids an SSM call and cold start per request. Reduces cost.

**SSM over Secrets Manager**
SSM Parameter Store SecureString is free tier for standard parameters. Secrets Manager costs $0.40/secret/month — unnecessary for a single shared key.

**Separate Lambda function for authorizer**
Keeps auth logic isolated from business logic. The authorizer Lambda is small (one class, minimal deps) and reuses the same Java 17 runtime.

## Risks / Trade-offs

- **Cold start on authorizer Lambda** → Mitigated by 300s cache TTL; only first request after cache miss pays the penalty
- **Shared secret = no revocation per caller** → Acceptable for POC scope
- **Key compromise requires SSM update** → Caller must use new key after rotation; no downtime

## Migration Plan

1. Provision `/bonbon/api-key` SecureString in SSM Parameter Store (manual or via CLI)
2. Deploy updated SAM stack (`sam deploy`)
3. Callers add `x-api-key: <value>` header to all requests
4. Rollback: remove authorizer from `template.yaml` and redeploy
