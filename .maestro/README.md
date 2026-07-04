# Maestro Smoke Tests

Automated UI smoke tests for the WooCommerce Android P2 checklist:
https://woomobilep2.wordpress.com/flows-for-app-features-smoke-testing/

## Operating Model

The suite has two store targets:

- `lab`: default for local development, repair loops, can-fail checks, and destructive iteration. Use a disposable Jurassic Ninja store with a dedicated test account.
- `shared`: `inpersonpayments.wpcomstaging.com`, used for release-tool runs, Thursday burst runs, and explicit non-destructive developer runs.

Destructive flows against the shared store are refused outside CI. In CI, the runner creates a REST-backed store lock before destructive shared-store runs and removes it on exit.

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

## Running

Default local run: lab store, seeded fixtures, `smoke_core` only, quarantine excluded.

```bash
.maestro/scripts/run-smoke-tests.sh --store lab
```

Common variants:

```bash
.maestro/scripts/run-smoke-tests.sh --device emulator-5554
.maestro/scripts/run-smoke-tests.sh --apk WooCommerce/build/outputs/apk/wasabi/debug/WooCommerce-wasabi-debug.apk
.maestro/scripts/run-smoke-tests.sh --include-tags smoke_extended --store lab
.maestro/scripts/run-smoke-tests.sh --include-tags flaky_quarantine .maestro/flows/orders_create.yaml
.maestro/scripts/run-smoke-tests.sh --store shared --include-tags smoke_core
.maestro/scripts/run-smoke-tests.sh --repeat 3 --store lab --include-tags smoke_core
```

The runner:

- selects one connected device automatically, or prompts when several are attached;
- captures and restores animation settings;
- seeds deterministic fixtures through the WooCommerce REST API;
- writes created entity IDs to `run-manifest.json`;
- deletes exactly those manifest IDs during cleanup;
- performs a guarded stale-orphan sweep for `SUITE-<date>-<hash>` entities older than 48h;
- retries each failed flow once and records pass-on-retry as flaky;
- redacts `MAESTRO_WOO_*` values from logs;
- stores artifacts outside the repo under `$HOME/woocommerce-maestro-output/<timestamp>/`.

## Tags

- `smoke_core`: stable non-destructive release signal paths.
- `smoke_extended`: broader P2 coverage.
- `pos_tablet`: POS flows, tablet AVD required.
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
