## Why

The HTTP API Gateway is publicly open — anyone with the endpoint URL can invoke the Lambda and accumulate Anthropic API costs. A Lambda Authorizer adds a lightweight custom authentication gate without requiring Cognito or IAM credentials from callers.

## What Changes

- New Lambda Authorizer function that validates an `x-api-key` header against a secret stored in SSM Parameter Store
- HTTP API Gateway configured to use the authorizer on the `POST /` route
- API key secret provisioned in SSM Parameter Store (`/bonbon/api-key`)
- IAM policy for the authorizer Lambda to read the key from SSM

## Capabilities

### New Capabilities
- `lambda-authorizer`: HTTP API Lambda Authorizer that validates a shared API key from the `x-api-key` request header

### Modified Capabilities
- `claude-invoke`: Route now requires a valid `x-api-key` header; requests without it are rejected with 401 before reaching the handler

## Impact

- `template.yaml`: Add authorizer Lambda function, `AWS::ApiGatewayV2::Authorizer` resource, attach to existing route
- New file: `src/main/java/com/bonbon/lambda/Authorizer.java`
- New SSM parameter: `/bonbon/api-key` (SecureString)
- No changes to `Handler.java` or `ClaudeClient.java`
