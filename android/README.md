# android/

Reserved boundary for the future Pastline Android client, written in Kotlin.

**Status:** post-V1. During V1 this directory contains only this README.

## Rules during V1

- no Android/Gradle project;
- no Android CI job;
- no mobile authentication implementation;
- no synchronization implementation;
- no mobile feature work that takes time from V1.

## Intended design after V1

- The Android client reuses the same Java/Spring backend and the same `/api/v1/...` business API as the web client. There is no separate mobile backend and no duplicated `/mobile/...` CRUD or search API.
- Mobile-specific endpoints are justified only for genuinely mobile use cases, such as a synchronization protocol or a mobile authentication exchange.
- Device-local AI is an Android client capability. It is not a Spring backend provider and does not run inside the Java backend.

See `PASTLINE_MASTER_PROMPT.md` Section 45.
