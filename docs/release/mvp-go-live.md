# MVP Go/No-Go

## Go Criteria

- [ ] Backend secret is stored server-side only.
- [ ] `/health` and `/version` endpoints pass checks.
- [ ] Server test/lint/build pipeline is green.
- [ ] Android unit tests pass.
- [ ] Manual E2E checklist completed on 5 devices.
- [ ] KPI targets met for 3 consecutive pilot days.

## No-Go Triggers

- API key leakage detected in mobile binary or logs.
- Crash rate > 0.5% in pilot.
- P50 latency > 3s for most sessions.
- OCR accuracy < 85% in target learning apps.

## Rollback Plan

1. Disable mobile access by rotating `CLIENT_TOKEN`.
2. Roll back backend image to previous tag.
3. Notify pilot users and collect failure logs.
