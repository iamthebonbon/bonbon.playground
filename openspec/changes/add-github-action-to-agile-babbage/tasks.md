## 1. GitHub Actions Workflow

- [x] 1.1 Create `.github/workflows/deploy.yml` with `workflow_dispatch` trigger, Java 17 (Corretto) setup, Maven package step, SAM build step, and SAM deploy step using `--no-confirm-changeset`
- [x] 1.2 Configure AWS credentials step using `aws-actions/configure-aws-credentials@v4` with `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_DEFAULT_REGION` secrets

## 2. GitHub Secrets Setup

- [ ] 2.1 Add `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_DEFAULT_REGION` secrets to the GitHub repository settings
- [x] 2.2 Verify the IAM user has least-privilege permissions: CloudFormation, S3, Lambda, IAM, SSM (read/write for SAM deploy)

## 3. Verification

- [ ] 3.1 Trigger the workflow manually from GitHub Actions UI and confirm the `sam-app` stack in `eu-north-1` is updated successfully
