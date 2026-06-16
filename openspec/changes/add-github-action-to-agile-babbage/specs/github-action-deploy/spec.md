## ADDED Requirements

### Requirement: Manual deploy workflow
The system SHALL provide a `workflow_dispatch`-triggered GitHub Actions workflow that builds the Maven project and deploys the SAM application to AWS without requiring local developer credentials.

#### Scenario: Developer triggers deploy
- **WHEN** a developer manually triggers the workflow from the GitHub Actions UI
- **THEN** the workflow runs `mvn clean package`, `sam build`, and `sam deploy --no-confirm-changeset` and succeeds

#### Scenario: Missing AWS credentials
- **WHEN** the required GitHub Secrets (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`) are not configured
- **THEN** the workflow fails at the AWS credentials step with a clear error

### Requirement: Deployment targets correct stack
The workflow SHALL deploy to the `sam-app` CloudFormation stack in `eu-north-1` using IAM capabilities matching the existing `samconfig.toml` configuration.

#### Scenario: Deploy uses correct region and stack
- **WHEN** the workflow completes successfully
- **THEN** the `sam-app` stack in `eu-north-1` reflects the latest code
