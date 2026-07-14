# Maestro Smoke Tests

Automated UI smoke tests for the WooCommerce Android P2 checklist:
https://woomobilep2.wordpress.com/flows-for-app-features-smoke-testing/

## Operating Model

The suite has two store targets:

- `lab`: default for local development, repair loops, can-fail checks, and destructive iteration. Use an
  automation-owned WooCommerce store that is connected to Jetpack/WP.com with a dedicated WP.com test account.
- `shared`: `inpersonpayments.wpcomstaging.com`, used for release-tool runs, Thursday burst runs, and explicit
  non-destructive developer runs.

The no-Jetpack login scenario uses its own `MAESTRO_WOO_NO_JETPACK_*` variables. Do not reuse those Jurassic Ninja
site credentials as the `lab` store block when running the broader suite.

Destructive flows against the shared store are refused outside CI. In CI, the runner creates a REST-backed store lock
before destructive shared-store runs and removes it on exit.

## Local Setup

Install prerequisites:

```bash
curl -fsSL "https://get.maestro.mobile.dev" | bash
adb devices
```

Create local credentials:

```bash
cp .maestro/env.example .maestro/.env.local
```

Fill `.maestro/.env.local` yourself from the canonical secret store. Do not paste credential values into agent conversations.
Validate the file before running flows, especially after pasting passwords:

```bash
.maestro/scripts/lint-env.py
```

Run the pre-flight doctor when setting up a machine, changing credentials, or preparing CI secrets:

```bash
.maestro/scripts/doctor.sh --profile phone-full --store lab --device emulator-5554
```

### Store data prerequisites

`orders_create` selects an existing live-store customer and edits only the customer copy attached to the order draft.
The app creates that `Order.Customer` in `OrderCreateEditCustomerAddFragment` and
`OrderCreateEditViewModel.onCustomerEdited` replaces only `orderDraft.customer`; it does not update the store customer.
The flow captures the selected email, verifies it on the draft, verifies the edited marker on the persisted order,
then searches the customer list again and requires the original email to be unchanged. The configured store must have
at least two existing customers with email addresses; missing data fails as an explicit prerequisite.

## Running

Default local run: lab store, `smoke_core` only, quarantine excluded.

```bash
.maestro/scripts/run-smoke-tests.sh --store lab
```

Common variants:

```bash
.maestro/scripts/run-smoke-tests.sh --profile core
.maestro/scripts/run-smoke-tests.sh --profile phone-full --device emulator-5554
.maestro/scripts/run-smoke-tests.sh --profile release
.maestro/scripts/run-smoke-tests.sh --profile burst
.maestro/scripts/run-smoke-tests.sh --profile pos-tablet --device Pixel_Tablet_API_35
.maestro/scripts/run-smoke-tests.sh --profile android-system --device Pixel_8_API_35
.maestro/scripts/doctor.sh --profile phone-full --store lab
.maestro/scripts/run-smoke-tests.sh --device emulator-5554
.maestro/scripts/run-smoke-tests.sh --apk WooCommerce/build/outputs/apk/wasabi/debug/WooCommerce-wasabi-debug.apk
.maestro/scripts/run-smoke-tests.sh --include-tags smoke_extended --store lab
.maestro/scripts/run-smoke-tests.sh --include-tags flaky_quarantine .maestro/flows/orders_create.yaml
.maestro/scripts/run-smoke-tests.sh --store shared --include-tags smoke_core
.maestro/scripts/run-smoke-tests.sh --repeat 3 --store lab --include-tags smoke_core
.maestro/scripts/run-smoke-tests.sh --rerun-failed ~/woocommerce-maestro-output/20260708141815/report.xml --store lab
```

Profiles are copy/paste-safe presets:

- `core`: lab store, `smoke_core`, quarantine and Android system surfaces excluded.
- `phone-full`: lab store, `smoke_core,smoke_extended`, tablet POS and Android system surfaces excluded. This includes quarantined phone flows.
- `release`: shared store, `smoke_core,smoke_extended,destructive`, quarantine, tablet POS, and Android system surfaces excluded.
- `burst`: same as `release`, repeated 3 times.
- `pos-tablet`: lab store, `pos_tablet`, quarantine included.
- `android-system`: lab store, `android_system`, quarantine included. Requires an English Pixel Launcher AVD with the
  Wasabi app discoverable as `Woo (Dev)` in the app drawer.

`--rerun-failed report.xml` reads failed/flaky JUnit test cases and runs only those flow files. It still honors
store, device, APK, repeat, and profile options.

The runner:

- selects one connected device automatically, or prompts when several are attached;
- captures and restores animation settings;
- can seed deterministic fixtures through the WooCommerce REST API when `--seed` is used;
- writes created entity IDs to `run-manifest.json` when seeding;
- deletes exactly those manifest IDs during cleanup when seeding;
- performs a guarded stale-orphan sweep for `SUITE-<date>-<hash>` entities older than 48h when seeding;
- retries each failed flow once and records pass-on-retry as flaky;
- redacts `MAESTRO_WOO_*` values from logs;
- stores artifacts outside the repo under `$HOME/woocommerce-maestro-output/<timestamp>/`;
- writes copy/paste commands into the HTML report for rerunning the same selection, rerunning failed flows, and running
  the doctor.

## Tags

- `smoke_core`: stable non-destructive release signal paths.
- `smoke_extended`: broader P2 coverage.
- `pos_tablet`: POS flows, tablet AVD required.
- `android_system`: launcher/system-surface flows, English Pixel Launcher AVD required.
- `destructive`: mutates store data.
- `flaky_quarantine`: provisional or unstable flows excluded from real runs.

The imported PR #15413 flows outside the four core paths are intentionally tagged `flaky_quarantine` until they graduate through the burst-based promotion policy.

## Coverage

Traceability is committed in `.maestro/smoke-coverage.yaml`. Each flow declares covered checklist items in a `# p2:` header.

Validate offline:

```bash
.maestro/scripts/check-smoke-coverage.py
```

Regenerate strings env after copy changes:

```bash
.maestro/scripts/generate-strings-env.py --check-flow-references
```

## Documentation

The self-contained system guide lives at `.maestro/docs/index.html`.
