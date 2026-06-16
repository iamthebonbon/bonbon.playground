## Context

No CI/CD pipeline exists. Developers deploy manually using local AWS credentials and `sam deploy`. The SAM stack (`sam-app`) targets `eu-north-1` with IAM capabilities.

## Goals / Non-Goals

**Goals:**
- Manual-trigger (`workflow_dispatch`) GitHub Actions workflow
- Build fat JAR with Maven, then build and deploy via SAM CLI
- AWS credentials stored as GitHub Secrets

**Non-Goals:**
- Automatic deploy on push
- Multi-environment or staging setup
- Test execution in CI (unit tests run locally)

## Decisions

**Use `workflow_dispatch` only** — manual trigger avoids accidental deploys. No branch filtering needed for a PoC.

**AWS credentials via GitHub Secrets** (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`) — simplest approach for a PoC; no OIDC role needed.

**`--no-confirm-changeset`** — overrides `samconfig.toml`'s `confirm_changeset = true` to allow unattended CI deploy.

**Java setup via `actions/setup-java@v4`** with Corretto 17 — matches Lambda runtime.

## Risks / Trade-offs

- [Long-lived IAM credentials in GitHub Secrets] → Use a least-privilege IAM user scoped to SAM/CloudFormation/S3/Lambda/IAM/SSM actions only
- [SAM deploy creates S3 bucket on first run] → Ensure IAM user has `s3:CreateBucket` permission
