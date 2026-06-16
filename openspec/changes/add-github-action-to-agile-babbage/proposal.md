## Why

No CI/CD pipeline exists. Deployments to AWS are manual and require local AWS credentials. A manual-trigger GitHub Action eliminates the need for developer machine setup and provides a consistent, repeatable deployment process.

## What Changes

- Add `.github/workflows/deploy.yml` — `workflow_dispatch`-triggered GitHub Action
- Stores AWS credentials and SAM parameters as GitHub Secrets
- Runs `mvn clean package` → `sam build` → `sam deploy`

## Capabilities

### New Capabilities
- `github-action-deploy`: Manual GitHub Actions workflow to build and deploy the SAM application to AWS (`eu-north-1`, stack `sam-app`)

### Modified Capabilities
<!-- none -->

## Impact

- New file: `.github/workflows/deploy.yml`
- Requires GitHub Secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
- No changes to existing Lambda source code or `template.yaml`
