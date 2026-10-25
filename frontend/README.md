# frontend/

Production boundary for the Pastline web client: React + TypeScript.

**Status:** empty boundary. The React application is generated in Week 3, not before.

## What belongs here

- the production React/TypeScript application;
- frontend-specific tests and configuration.

## Boundary rules

- React is one API client of the shared backend. Journal and search business rules stay in the backend, not here.
- All calls go through one HTTP abstraction against the versioned `/api/v1` API.
- Web V1 uses server-side AI only. The UI must not claim device-local AI support.

See `PASTLINE_MASTER_PROMPT.md` Sections 14, 42 and 43.
