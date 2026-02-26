# Manual E2E Checklist (MVP)

## Environment

- Device models: at least 5
- Android versions: 10 / 12 / 13 / 14
- Backend: `/health` returns `{"ok":true}`

## Checklist

1. First launch shows permission onboarding.
2. Overlay permission granted and bubble appears.
3. Projection permission granted and capture flow starts.
4. Notification permission granted on Android 13+.
5. Bubble click triggers "识别中..." state.
6. Success response renders word and explanation.
7. Copy button writes text to clipboard.
8. Ambiguous candidate message appears when OCR confidence is low.
9. Network failure shows deterministic error message.
10. App does not crash when overlay is toggled repeatedly.

## KPI Capture

- P50 end-to-end latency <= 3s
- OCR target word accuracy >= 85%
- Copy action success >= 99%
