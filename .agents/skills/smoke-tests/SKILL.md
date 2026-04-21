---
name: smoke-tests
description: Prepare the local environment for the Maestro smoke-test suite — verify tooling, ensure an emulator is ready, collect the APK from the user, validate the .env file — then hand the user the exact CLI command to run (or run it for them on request).
allowed-tools: Bash, Read, Edit, Write, Grep, Glob, AskUserQuestion
user-invocable: true
---

# Prepare & launch the Maestro smoke-test suite

This skill is a **setup + handoff** flow. Its job is to get everything ready so `.maestro/scripts/run-smoke-tests.sh` will work on the first try, then give the user the CLI command.

It does NOT own the test-runner mechanics. Ordering, per-flow recording, report generation, and the "recordings are kept only for failures, outside the repo" contract all live in the script itself.

## Scope (what this skill is responsible for)

1. **Tooling** — confirm `maestro` and `adb` are on PATH. If not, tell the user how to install them and stop.
2. **Emulator** — confirm at least one Android device/emulator is attached (`adb devices`). If none, help boot an AVD.
3. **APK** — ask the user which APK to install and run against; install it.
4. **`.env.local`** — confirm `.maestro/.env.local` exists and contains every variable the current flows reference.
5. **Handoff** — print the exact CLI command (with output-dir flag already populated). If the user explicitly asks ("run it", "go ahead", etc.), invoke the script for them and stream its output.

Everything below the handoff — P2 ordering, screen recording per flow, keeping only failure artifacts outside the repo, HTML + JUnit reports — is handled by `.maestro/scripts/run-smoke-tests.sh`.

## Steps

### 1. Check tooling

Run `command -v maestro` and `command -v adb`. If either is missing:

- maestro → `curl -fsSL "https://get.maestro.mobile.dev" | bash` (requires Java 17+).
- adb → install Android SDK platform-tools and add to PATH.

Tell the user which is missing and how to install it, then stop.

### 2. Ensure an emulator is running

Run `adb devices`. Count the lines whose second column is `device`.

- **0 devices:** list AVDs with `emulator -list-avds`.
  - If none → tell the user to create one in Android Studio (AVD Manager) and stop.
  - If exactly one → ask the user if they want to boot it, then `emulator -avd <name> -no-snapshot-save &` (backgrounded), `adb wait-for-device`, and poll `adb shell getprop sys.boot_completed` until `1` (up to ~90s).
  - If multiple → ask which one to boot.
- **1+ devices:** proceed.

### 3. Ask the user for the APK (do this up front, every run)

Use `AskUserQuestion` as the first user-facing step. Do NOT assume the previous install on the emulator is the right build.

Prompt the user for one of:

- An absolute or repo-relative path to an `.apk` on disk.
- A drag-and-drop attachment (Claude Code exposes it as a temp path like `/tmp/.../file.apk`).
- The word `build` — in which case run `./gradlew :WooCommerce:assembleWasabiDebug` in the foreground and use `WooCommerce/build/outputs/apk/wasabi/debug/WooCommerce-wasabi-debug.apk`.

Validate the chosen APK:

- File exists on disk.
- Extension is `.apk`.
- `aapt dump badging <path> | head -1` reports `package: name='com.woocommerce.android.dev'` (the wasabi build). If it reports a different package, stop and ask the user whether to continue — a non-wasabi APK will fail the `appId` check on the first flow.

Install it: `adb install -r -g <apk-path>`. If the install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, tell the user a previous build with a different signature is already installed and ask whether to uninstall first (`adb uninstall com.woocommerce.android.dev`). Do not uninstall without confirmation.

### 4. Validate `.maestro/.env.local`

The required vars are:

- `MAESTRO_WOO_EMAIL`
- `MAESTRO_WOO_PASSWORD`
- `MAESTRO_WOO_STORE_URL`

Additional `MAESTRO_WOO_*` vars are required per-flow (e.g. `MAESTRO_WOO_NOT_A_WOO_STORE_URL` for `login_not_woo_store.yaml`, `MAESTRO_WOO_JN_*` for `login_no_jetpack.yaml`). The canonical list lives in `.maestro/env.example`.

To build the list of actually-referenced vars, grep flow YAMLs for `\${WOO_[A-Z_]+}` under `.maestro/flows/` and check each one resolves to a non-empty value in `.maestro/.env.local`.

If the file is missing or any required var is empty:

1. List the missing variable names and their purpose (mirror `.maestro/env.example`).
2. Ask the user whether they want to paste the values in chat or edit `.maestro/.env.local` directly.
3. Do NOT proceed until every referenced var resolves. Re-read the file after the user updates it.

Never commit `.env.local`. Never echo secret values back to the user.

### 5. Hand off the CLI command

Once steps 1–4 all pass, print the command the user should run:

```
.maestro/scripts/run-smoke-tests.sh
```

Tell the user:

- The script runs every flow in the exact P2 order (error-path login flows first, then `login_successful` — which leaves the app logged in — then the rest of the suite in Dashboard → Orders → Products → Hub Menu → Blaze → Google for Woo → POS order).
- Screen recordings are kept only for flows that fail.
- Artifacts (recordings, logs, HTML + JUnit report) are written OUTSIDE the repo, under `$HOME/woocommerce-maestro-output/<timestamp>/` by default. Override with `--output-dir <path>` or the `WOO_MAESTRO_OUTPUT_DIR` env var.
- The HTML report auto-opens at the end on macOS, or can be opened manually from the path the script prints.

If the user asks to run it ("go ahead", "run it", "yes please", etc.), invoke the script yourself via `Bash` and stream its output. Do NOT pass `--apk` — the APK was already installed in step 3, and `--apk` would reinstall unnecessarily.

When the script exits, read the last line of its output (it prints `Report:` and `Result:` summary lines) and relay a one-line summary to the user plus the clickable `file://` path to the HTML report. If any flows failed, call out the first failing flow by name — it's usually the most actionable one.

## Notes

- The skill's job ends at handoff. Don't reinvent the test-runner behaviour here — if something about per-flow recording, ordering, or artifact location needs to change, change it in `.maestro/scripts/run-smoke-tests.sh`.
- `.env.local` is git-ignored. Never stage or commit it, even if the user asks you to save their credentials.
- Artifacts default to `$HOME/woocommerce-maestro-output/` (outside the repo) — the repo's `.gitignore` still excludes the legacy `.maestro/output/` path for safety.
- Do NOT parallelize. `adb shell screenrecord` only supports one invocation per device, and Maestro runs one flow at a time against a single emulator. The script runs sequentially by design.
