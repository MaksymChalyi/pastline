# scripts/

Repeatable project automation. A script belongs here only if it will be run again, for example the synthetic corpus generator or an experiment runner.

**Status:** empty.

Planned (from the roadmap, not yet created):

- `scripts/generate-synthetic-corpus`: generates the public evaluation corpus with a fixed seed (Section 15A.4).

## Rules

- No random one-off code. One-off experiments belong in `labs/`.
- Scripts must not read from or write to the private corpus of real entries in a way that could commit it.
- Document every script's command and inputs in this README when it is added.
