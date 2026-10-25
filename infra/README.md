# infra/

Deployment and infrastructure material only.

**Status:** empty. Nothing here is executable yet.

Planned contents:

- `infra/docker/`: backend Docker image material, added when the backend can be built (Section 36);
- `infra/aws/`: AWS deployment notes and configuration for Week 11 (Sections 34-35).

The local development `docker-compose.yml` is created in Week 2 (Checkpoint 2.1/2.3), when PostgreSQL becomes executable infrastructure. It is deliberately not created during Phase 0.

## Rules

- No secrets, credentials or real passwords. Secrets live in a secret store or a local ignored `.env`.
- Do not add configuration files before they can be run and verified.
