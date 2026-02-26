# ADR-0001: Two-Tier Architecture (Mobile + Secure Proxy)

## Status

Accepted (2026-02-26)

## Context

The app needs a fast floating assistant during vocabulary learning while keeping provider API keys out of the Android package.

## Decision

Use:

- Android client for overlay, capture, OCR, and UX.
- Backend proxy for model invocation, validation, auth, and rate limiting.

## Consequences

- Better security posture: keys stored server-side only.
- Better governance: prompt policy and rate limits centralized.
- Slightly higher deployment complexity due to server component.
