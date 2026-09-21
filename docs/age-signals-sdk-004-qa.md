# Play Age Signals SDK 0.0.4 release validation

This checklist is the release-proof template for
[WOOMOB-3726](https://linear.app/a8c/issue/WOOMOB-3726/android-migrate-play-age-signals-integration-to-sdk-004).
Attach completed evidence to that parent issue before enabling the migration in production.

## Release gates

- [ ] [WOOMOB-3769](https://linear.app/a8c/issue/WOOMOB-3769/policy-confirm-woo-scope-for-play-significant-change-denial)
      contains Product/Legal's final decision and links the final P2 comment to WOOMOB-3726.
- [ ] Play Console configuration has been checked for custom age ranges that could invalidate Woo's 13-year boundary.
- [ ] A Play-installed internal-testing build has completed every applicable scenario below.
- [ ] QA evidence is attached to WOOMOB-3726 and identifies the tested build, device, Android version, account state,
      and Play configuration.
- [ ] The four-PR stack is approved and will merge bottom-up so the dormant verification UI and SDK activation ship
      in the same release.

## Test environment

Do not use a sideloaded APK for acceptance. Google documents `APP_NOT_OWNED` as a legitimate result when the app was
not acquired from Google Play.

- Distribution: Google Play internal testing
- Device: Play-certified Android 6.0 (API 23) or newer
- Build/version:
- Device and Android version:
- Google Play Store and Play services versions:
- Woo account/site used:
- `AGE_ELIGIBILITY_CHECKS` state:
- Play Console age-range configuration checked by/date:

## Functional scenarios

Record a result and evidence link for each row. Run cold-start scenarios both logged in and logged out.

| Scenario | Expected result | Result | Evidence |
|---|---|---|---|
| Logged-in cold start | The check runs after the Activity is resumed; normal eligible access is unchanged. |  |  |
| Logged-out cold start | The check runs without requiring a selected site and does not disrupt login. |  |  |
| In-app sharing prompt | The Google-owned prompt can complete and Woo handles the returned access state. |  |  |
| `NOT_SHARED` without prior restriction | Woo remains usable, does not log out, and records a non-authoritative result. |  |  |
| `NOT_SHARED` with prior restriction | The authoritative restriction remains and is not cleared. |  |  |
| `VERIFICATION_REQUIRED` | A shared non-cancelable gate appears in Main or Login without logout or selected-site reset. |  |  |
| Open Play Store and return | `market://` opens, or HTTPS is used as fallback; exactly one retry runs on resume. |  |  |
| Repeated verification or retry failure | The gate remains visible and Retry can start another attempt. |  |  |
| Shared age 0–12 | The under-13 restriction is persisted, logout occurs, and the existing terminal message appears. |  |  |
| Shared age 13–15 | Access is allowed and a prior authoritative restriction is cleared. |  |  |
| Shared age 16–17 | Access is allowed and a prior authoritative restriction is cleared. |  |  |
| Shared age 18+ | Access is allowed and a prior authoritative restriction is cleared. |  |  |
| Missing lower bound with an upper bound below 13 | The under-13 restriction is persisted and logout occurs. |  |  |
| Missing both bounds, crossing bounds with an upper bound of 13+, or an age range crossing 13 | A prior restriction is retained; otherwise access is allowed as an ambiguous result. |  |  |
| Nonstandard range with a lower bound of at least 13 | Access is allowed and telemetry records an eligible range outcome. |  |  |
| Significant change `PENDING` or `DECLINED` | The state is recorded but does not block the whole app. |  |  |
| Offline or terminal SDK/API error | A prior restriction is retained; otherwise normal Woo access continues. |  |  |
| Transient error recovery | At most three attempts occur, with 500 ms and 1 s backoff. |  |  |
| Process death while gated | The restored/cold-start flow returns to the verification gate until an allowed result is received. |  |  |
| Back gesture and outside tap | Neither dismisses the verification gate. |  |  |
| Large font and screen reader | Content scrolls, actions remain reachable, and labels/order are understandable. |  |  |
| Feature flag disabled | The persisted restriction is bypassed without being erased; this is the rollback path. |  |  |

## Analytics and privacy

- [ ] `account_age_restriction_checked` contains only categorical request stage, access status, age band,
      significant-change status, final decision, restriction reason, SDK error, retry count, and recovery state.
- [ ] `account_age_verification_action` records `open_play_store`, `manual_retry`, and `return_from_play_retry`.
- [ ] No event or log evidence contains raw bounds, install ID, approval dates, exception messages, or exact user IDs.
- [ ] Unexpected access values and SDK errors are visible as bounded categories for rollout monitoring.

## Rollout

- Use the normal staged rollout with `AGE_ELIGIBILITY_CHECKS` as the rollback switch.
- Monitor access-status distribution, unexpected statuses, SDK error categories, retry counts, verification recovery,
  and restriction-triggered logout.
- Pause rollout on evidence of false-positive merchant lockouts.
