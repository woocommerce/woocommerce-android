---
name: smoke-tests
description: Prepare the local environment for the Maestro smoke-test suite — verify tooling, select a device, collect the APK if needed, validate the .env file by variable name only, then hand the user the exact lab-store CLI command to run or run it on request.
allowed-tools: Bash, Read, Edit, Write, Grep, Glob, AskUserQuestion
user-invocable: true
---

# Prepare & launch the Maestro smoke-test suite

This skill is a **setup + handoff** flow. Its job is to get everything ready so `.maestro/scripts/run-smoke-tests.sh` will work on the first try, then give the user the CLI command.

It does NOT own the test-runner mechanics. Ordering, per-flow recording, report generation, and the "recordings are kept only for failures, outside the repo" contract all live in the script itself.

## Scope (what this skill is responsible for)

1. **Tooling** — set up the pinned Maestro and Java with `.maestro/scripts/configure-toolchain.sh`, and confirm `adb` is on PATH.
2. **Device** — confirm at least one Android device/emulator is attached (`adb devices`). If several are attached, choose the runner `--device` value with the user.
3. **APK** — the runner installs the latest release by itself; ask only whether the user wants a local release build instead.
4. **`.env.local`** — check it with `lint-env.py` and the doctor, which name missing variables without printing values.
5. **Handoff** — print the exact CLI command. If the user explicitly asks ("run it", "go ahead", etc.), invoke the script for them and stream its output.

Everything below the handoff — P2 ordering, store selection, seed/cleanup, animation restore, retry accounting, artifact policy, HTML + JUnit reports — is handled by `.maestro/scripts/run-smoke-tests.sh`.

## Steps

### 1. Check tooling

Run `source .maestro/scripts/configure-toolchain.sh`. It installs the pinned Maestro 2.9.0 into the workspace, selects Java 21 and fails with a clear message when either is missing. Then run `command -v adb`; if adb is missing, tell the user to install the Android SDK platform-tools and stop.

Each Bash call starts a new shell, so put `source .maestro/scripts/configure-toolchain.sh &&` in front of every later command that runs Maestro, the doctor or the runner.

### 2. Ensure an emulator is running

Run `adb devices`. Count the lines whose second column is `device`.

- **0 devices:** list AVDs with `emulator -list-avds`.
  - If none → tell the user to create one in Android Studio (AVD Manager) and stop.
  - If exactly one → ask the user if they want to boot it, then `emulator -avd <name> -no-snapshot-save &` (backgrounded). Find its `emulator-*` serial in `adb devices` and poll `adb -s <serial> shell getprop sys.boot_completed` until `1` (up to ~90s); a phone on wireless adb can be attached too.
  - If multiple → ask which one to boot.
- **1+ devices:** proceed.

### 3. Choose the app build

The runner needs the non-debuggable production package, `com.woocommerce.android`. When it is missing from the device, the runner downloads and installs the latest stable GitHub release, so there is nothing to do by default.

Ask whether the user wants to test the current checkout instead. If so, run `./gradlew :WooCommerce:assembleVanillaRelease` in the foreground and pass `--apk WooCommerce/build/outputs/apk/vanilla/release/WooCommerce-vanilla-release.apk` to the runner. Debug and wasabi builds are rejected.

### 4. Validate `.maestro/.env.local`

Run `.maestro/scripts/lint-env.py`, then `source .maestro/scripts/configure-toolchain.sh && .maestro/scripts/doctor.sh --profile <profile> --seed --device <serial>`. Both name missing or malformed variables without printing their values, and the doctor checks the store block each flow needs: flows tagged `store_shared` use the `MAESTRO_WOO_SHARED_*` block, every other flow the `MAESTRO_WOO_LAB_*` block. `.maestro/env.example` lists every variable and what it is for.

If anything is missing:

1. List the variable names the doctor reported.
2. Ask the user to edit `.maestro/.env.local` directly.
3. Run the doctor again; do not read the file yourself.

Never commit `.env.local`. Never ask the user to paste secret values into chat. Never echo secret values back to the user.

### 5. Hand off the CLI command

Once steps 1–4 all pass, print the command the user should run:

```
source .maestro/scripts/configure-toolchain.sh && .maestro/scripts/run-smoke-tests.sh --profile phone-full --seed --device <serial>
```

Tell the user:

- Without `--profile`, the runner runs `smoke_core` only and excludes `flaky_quarantine`.
- Each flow runs against the store it needs: `store_shared` flows on the shared store, the rest on the lab store. `--store lab|shared` forces one store for every flow, and the runner refuses destructive flows on the shared store.
- Destructive flows need `--seed`. It seeds their fixtures, and cleanup deletes what the run created when it exits.
- It captures and restores device animation settings.
- Screen recordings are kept only for failed lab-store flows; shared-store credential paths use screenshots only.
- Artifacts (recordings, logs, HTML + JUnit report) are written OUTSIDE the repo, under `$HOME/woocommerce-maestro-output/<timestamp>/` by default. Override with `--output-dir <path>` or the `WOO_MAESTRO_OUTPUT_DIR` env var.
- The HTML report auto-opens at the end on macOS, or can be opened manually from the path the script prints.

If the user asks to run it ("go ahead", "run it", "yes please", etc.), invoke the script yourself via `Bash` and stream its output. Pass `--apk` only if the user chose a local build in step 3.

When the script exits, read the last line of its output (it prints `Report:` and `Result:` summary lines) and relay a one-line summary to the user plus the clickable `file://` path to the HTML report. If any flows failed, call out the first failing flow by name — it's usually the most actionable one.

## Notes

- The skill's job ends at handoff. Don't reinvent the test-runner behaviour here — if something about per-flow recording, ordering, or artifact location needs to change, change it in `.maestro/scripts/run-smoke-tests.sh`.
- `.env.local` is git-ignored. Never stage or commit it, even if the user asks you to save their credentials.
- Artifacts default to `$HOME/woocommerce-maestro-output/` (outside the repo) — the repo's `.gitignore` still excludes the legacy `.maestro/output/` path for safety.
- Do NOT parallelize. `adb shell screenrecord` only supports one invocation per device, and Maestro runs one flow at a time against a single emulator. The script runs sequentially by design.
