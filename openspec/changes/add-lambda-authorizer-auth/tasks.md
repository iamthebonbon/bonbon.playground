## 1. SSM Parameter

- [ ] 1.1 Create `/bonbon/api-key` as a SecureString in SSM Parameter Store (AWS Console or CLI)

## 2. Authorizer Lambda

- [x] 2.1 Create `src/main/java/com/bonbon/lambda/Authorizer.java` implementing `RequestHandler<APIGatewayV2CustomAuthorizerV2Request, APIGatewayV2CustomAuthorizerSimpleResponse>`
- [x] 2.2 In static initializer: fetch `/bonbon/api-key` from SSM with decryption and cache in a static field
- [x] 2.3 In `handleRequest`: read `x-api-key` from `event.getHeaders()`, compare to cached secret, return `isAuthorized: true/false`

## 3. SAM Template

- [x] 3.1 Add `AuthorizerFunction` resource to `template.yaml` with handler `com.bonbon.lambda.Authorizer::handleRequest` and SSM policy for `/bonbon/api-key`
- [x] 3.2 Add `AWS::ApiGatewayV2::Authorizer` resource: type `REQUEST`, payload format `2.0`, `AuthorizerResultTtlInSeconds: 300`, identity source `$request.header.x-api-key`
- [x] 3.3 Add `AWS::ApiGatewayV2::Route` override for `POST /` to attach the authorizer (or use `Auth` property on the SAM `HttpApi` event)

## 4. Verification

- [ ] 4.1 Run `sam build && sam deploy` and confirm stack deploys without errors
- [ ] 4.2 Send `curl -X POST <ApiUrl> -H "Content-Type: application/json" -H "x-api-key: <secret>" -d '{"prompt":"hello"}'` — expect HTTP 200
- [ ] 4.3 Send the same request without the `x-api-key` header — expect HTTP 403
- [ ] 4.4 Send with a wrong key value — expect HTTP 403