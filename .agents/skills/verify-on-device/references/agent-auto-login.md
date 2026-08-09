# Agent auto-login

`tools/agent-login/agent-login.sh` sends a preconfigured login request to an installed debuggable WooCommerce
Android build. It is a happy-path development tool for test accounts. Credentials are read from a local profile and
are never accepted as command-line arguments.

Supported targets:

| Flavor | Package |
|---|---|
| `dev` | `com.woocommerce.android.dev` |
| `prealpha` | `com.woocommerce.android.prealpha` |

All debug variants expose the DUMP-protected auto-login Activity. The host script supports the `dev` and
`prealpha` packages listed above. Release packages do not expose the Activity.

## Profile

The script uses the deterministic `default.json` profile unless `--profile NAME` selects a named override. It never
searches or lists the profile directory.

Store profiles outside the repository:

- `$XDG_CONFIG_HOME/woocommerce-android/auto-login/profiles/<name>.json` when `XDG_CONFIG_HOME` is set.
- `$HOME/.config/woocommerce-android/auto-login/profiles/<name>.json` otherwise.

Create the default profile once with a local editor. This keeps credentials out of shell arguments and history:

```bash
config_home="${XDG_CONFIG_HOME:-$HOME/.config}"
profile_dir="$config_home/woocommerce-android/auto-login/profiles"
umask 077
mkdir -p "$profile_dir"
chmod 700 "$profile_dir"
touch "$profile_dir/default.json"
chmod 600 "$profile_dir/default.json"
"${EDITOR:-vi}" "$profile_dir/default.json"
```

The profile must be stored outside the repository and be a regular, readable file with mode `0600`, between 1 byte
and 16 KiB. The Android app validates the JSON request:

```json
{
  "connection": "WPCOM",
  "site_url": "https://example.invalid",
  "username": "<account name>",
  "password": "<dedicated application password>"
}
```

`connection` is `WP_API` or `WPCOM`. Use a dedicated revocable WordPress Application Password for `WP_API` and a
dedicated revocable WordPress.com Application Password for `WPCOM`. Do not put a primary password in an automation
profile or share profile contents in chat.

The default profile is `default.json`. Override it with `--profile NAME` when the user or task explicitly supplies a
different nonsecret profile name. Names may contain letters, digits, `.`, `_`, and `-`, must begin with a letter or
digit, and are limited to 64 characters.

## Run

With exactly one authorized device:

```bash
tools/agent-login/agent-login.sh --flavor dev
```

Select a device and Android user explicitly when needed:

```bash
tools/agent-login/agent-login.sh \
  --flavor prealpha \
  --serial emulator-5554 \
  --user 0
```

Use a named override only when it was explicitly configured:

```bash
tools/agent-login/agent-login.sh --profile agent-test --flavor dev
```

Without `--user`, the script uses the device's current Android user. The selected debug package must be installed and
support `run-as`.

Operational outcomes print one sanitized stdout token. Usage errors print a diagnostic or usage text on stderr and
no outcome token. Diagnostics never include the profile content, profile path, site URL, username, or password.

## Transport

The script sends the profile bytes only through adb stdin. A fixed inline `run-as` command writes them into the app's
private no-backup directory:

```text
no_backup/auto-login/request.tmp
no_backup/auto-login/request.ready
```

The remote command removes stale request and status files, reads stdin into `request.tmp`, verifies the 1–16384 byte
size, and renames it to `request.ready` only after reaching EOF. It then launches the explicit debug Activity with
action `com.woocommerce.android.debug.AUTO_LOGIN`. The Intent contains no data or extras.

The Activity is protected by `android.permission.DUMP`. It consumes and deletes `request.ready`, performs login, and
atomically publishes one sanitized outcome name to:

```text
no_backup/auto-login/status.ready
```

The host polls that file, validates the complete value against the known outcome set, reports it, and removes the
fixed request/status files. The transport does not use a URI, `adb push`, `/data/local/tmp`, Intent credentials,
Gradle properties, `BuildConfig`, or UI keystrokes.

This deliberately has no UUIDs, framing protocol, TTL, queue, or concurrency support. Do not run it concurrently
with another agent login, manual login, or another authentication flow. A new invocation replaces stale fixed
request/status files.

A disconnect or other failure during status polling, malformed status, or poll timeout produces `OUTCOME_UNKNOWN`.
Do not retry that outcome automatically because the login may already have changed app state. An authorized adb host,
root, debugger, or code running as the app UID is outside this development tool's threat model.

## Outcomes

| Exit | Outcome | Meaning |
|---:|---|---|
| 0 | `SUCCESS`, `ALREADY_ACTIVE` | Login succeeded, or the requested target and connection were already active. |
| 10 | usage error | A nonsecret argument is missing, malformed, or unsupported. |
| 11 | `PROFILE_ERROR` | The default or selected local profile is unavailable, has the wrong mode, or has an invalid size. |
| 12 | `DEVICE_SELECTION_REQUIRED`, `DEVICE_UNAVAILABLE` | Device selection, authorization, or user resolution failed. |
| 13 | `TARGET_UNAVAILABLE` | The package is absent or does not support `run-as`. |
| 15 | `STAGE_FAILED` | Private stdin staging failed before launch. |
| 16 | `START_FAILED` | The protected Activity could not be started. |
| 17 | `OUTCOME_UNKNOWN` | Status polling, status validation, a disconnect during polling, or timeout made the outcome unknowable. |
| 20 | `CONFLICT` | Existing account state conflicts; replacement is unsupported. |
| 22 | `INVALID_REQUEST` | The Android app rejected the profile JSON or invocation. |
| 24 | `AUTH_REQUIRES_2FA` | WordPress.com requested 2FA; OTP automation is unsupported. |
| 25 | `AUTH_FAILED` | Authentication or account fetching failed. |
| 26 | `SITE_FAILED` | Site refresh or resolution failed. |
| 30 | `INTERNAL_ERROR` | The app failed internally with a sanitized result. |

`WP_API` discovers the site, stores the supplied Application Password, and makes an authenticated user request.
`WPCOM` authenticates, fetches the account, and reuses the production Jetpack repository to refresh and select the
requested WordPress.com REST site.

The tool assumes the configured target is connected, the account has the correct role and eligibility, and
WordPress.com does not require 2FA. The app may surface store configuration errors after login. Failed attempts may
leave partial debug session state; preserve the session for manual recovery unless the task explicitly requires a
clean reset.

## Verification

Local Android tests cover request parsing, credential redaction, routing, both authentication strategies, and
protected Activity navigation. For a manual host smoke check, use a mode-`0600` test profile, run the script, and
confirm the selected store opens. When changing the entry point, inspect merged manifests: the Activity must be
DUMP-protected and filter-free in every debug variant and absent from release builds.
