---
name: smoke-tests
description: Run the full Maestro smoke-test suite on an Android emulator with per-flow video recording and an HTML failure report
allowed-tools: Bash, Read, Edit, Write, Grep, Glob, AskUserQuestion
user-invocable: true
---

# Run Maestro Smoke Tests

Orchestrate a full run of the Maestro smoke-test suite (everything under `.maestro/flows/`) on an Android emulator, record each flow as it runs, discard recordings for passing flows, and hand the user an HTML report that links the failure videos with short troubleshooting hints.

**Prerequisites**
- Maestro CLI on PATH (`curl -fsSL "https://get.maestro.mobile.dev" | bash`)
- Android SDK platform-tools on PATH (`adb`, `emulator`)
- At least one Android Virtual Device created in Android Studio

## Steps

### 1. Ask the user for the APK

Ask for the APK that should be under test. Accept either:
- **A file path** — an already-built APK somewhere on disk (absolute or relative to the repo root).
- **An attached APK** — if the user dragged/attached an `.apk` file to the chat, Claude Code exposes it under a temp path in the conversation. Read that path and treat it as the APK.

If the user doesn't provide either, ask them to build one first with `./gradlew :WooCommerce:assembleWasabiDebug` and pass back the path from `WooCommerce/build/outputs/apk/wasabi/debug/`.

Validate the APK exists and its filename ends in `.apk` before moving on.

### 2. Validate the Maestro env file

Required env vars live in `.maestro/.env.local` (git-ignored). Check that the file exists and that every variable the current flows reference is populated.

- Read `.maestro/env.example` for the canonical list. The variables marked REQUIRED at the top of that file are the minimum: `MAESTRO_WOO_EMAIL`, `MAESTRO_WOO_PASSWORD`, `MAESTRO_WOO_STORE_URL`.
- Also check every `MAESTRO_WOO_*` variable that is referenced by at least one flow under `.maestro/flows/`. Grep flow YAMLs for `${WOO_*}` patterns to build the list; for each one, verify the matching `MAESTRO_WOO_*` is set in `.env.local`.

If anything is missing:
1. List the missing variables and their purpose (mirror the comments in `.maestro/env.example`).
2. Ask the user to either paste the values in chat OR edit `.maestro/.env.local` directly.
3. Do NOT proceed until all referenced variables resolve to non-empty values. Re-read the file after the user updates it.

Never commit `.env.local` or echo secret values back to the user.

### 3. Ensure an emulator is running

Check `adb devices`. If no device line has state `device`:
1. List available AVDs with `emulator -list-avds`.
2. If none exist, tell the user to create one in Android Studio (AVD Manager) and stop.
3. If multiple AVDs exist, ask the user which to boot.
4. Boot with `emulator -avd <name> -no-snapshot-save &` (backgrounded) and wait for `adb wait-for-device` to return, then poll `adb shell getprop sys.boot_completed` until it returns `1` (up to ~90 seconds).

If exactly one device is already connected, use that one and skip the boot step.

### 4. Install the APK

`adb install -r -g <apk-path>`.

The `-r` reinstalls over any existing install without clearing state; `-g` grants runtime permissions upfront so Maestro isn't blocked by permission dialogs.

If the install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, tell the user a previous build with a different signature is installed and ask whether to `adb uninstall com.woocommerce.android.dev` first. Don't uninstall without confirmation — they may have state they want.

### 5. Run the suite

Invoke the recording wrapper:

```
.maestro/scripts/run-smoke-tests-recorded.sh --no-open
```

That script handles everything from here: it runs the login flows first, then the rest; starts an `adb shell screenrecord` around each flow; deletes the recording if the flow passed, keeps it if it failed; and produces `.maestro/output/<timestamp>/report.html` with the failures, embedded videos, and inline troubleshooting tips.

Do NOT use `--apk` on the wrapper — you already installed it in step 4. Passing `--apk` would reinstall unnecessarily.

Stream the wrapper's output so the user can see progress. The whole suite typically takes 20–40 minutes depending on device speed.

### 6. Share the report

When the wrapper exits, locate the most recent `.maestro/output/<timestamp>/report.html`. Print a clickable `file://` link to it for the user, plus a one-line summary (e.g. `14 passed, 3 failed in 23m`).

If failures exist, also call out the first failing flow by name — it's usually the most actionable one, and login-suite failures in particular cascade into every downstream flow.

Briefly mention that recordings for passing flows were deleted to save space, and that each failure's section in the HTML report has an embedded video, the Maestro error line, and likely-causes hints.

## Notes

- **Do not parallelize.** `adb shell screenrecord` can only run one process per device, and a single emulator can only run one Maestro flow at a time. The wrapper runs sequentially by design.
- **3-minute recording cap.** Android's `screenrecord` hard-limits each invocation to 180 seconds. Every current smoke flow finishes well under that. If a new flow ever exceeds it, the recording will be truncated but the flow result is unaffected.
- **Login flows run first.** The wrapper orders `login_*` flows before everything else. If login breaks, the rest of the suite is guaranteed to fail with irrelevant errors, so seeing those failures at the top of the report is intentional.
- **The `.env.local` file is git-ignored.** Never stage or commit it, even if the user asks you to save their credentials.
