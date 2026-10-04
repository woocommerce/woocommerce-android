---
name: maestro-flow-doctor
description: Repair a failing WooCommerce Android Maestro flow by reproducing it against the store it runs on, inspecting Maestro selectors, patching the smallest selector/wait/setup issue, and rerunning with repeat evidence. Human-triggered only.
allowed-tools: Bash, Read, Edit, Grep, Glob
user-invocable: true
---

# Maestro Flow Doctor

Use this skill only for repairing existing Maestro smoke flows under `.maestro/flows/`.

## Ground Rules

- Run each flow against the store it declares; don't pass `--store`. Flows tagged `store_shared` need the shared store's second store and Google for WooCommerce.
- Never run destructive repair loops against the shared store; the runner refuses them.
- Never ask the user to paste credentials. Validate that `.maestro/.env.local` has the required variable names without echoing values.
- Use Maestro MCP as the selector source of truth: `run` executes the YAML we ship, and `inspect_screen` shows the hierarchy Maestro selectors see.
- Keep fixes minimal: selector, wait, setup, or fixture query changes only. Do not broaden coverage while repairing a flake.

## Workflow

1. Confirm the target flow path exists under `.maestro/flows/`.
2. Run syntax and coverage checks:
   - `.maestro/scripts/check-smoke-coverage.py`
   - `maestro check-syntax <flow>`
3. Reproduce on a device:
   - `.maestro/scripts/run-smoke-tests.sh --seed --include-tags flaky_quarantine --device <serial> <flow>`
4. At the failure point, use Maestro MCP `inspect_screen`.
5. Compare the failing selector with the hierarchy:
   - Prefer `id:` selectors exposed through `testTag`.
   - Use generated `strings.env` values for text assertions.
   - Do not add `point:` selectors unless the flow comment explains why no semantic selector exists.
6. Patch the smallest file set.
7. Rerun the single flow with repeat evidence:
   - Non-destructive flow: `.maestro/scripts/run-smoke-tests.sh --repeat 3 --device <serial> <flow>`
   - Destructive flow, which uses up the fixtures of one seed, so it runs once per seed:
     `for i in 1 2 3; do .maestro/scripts/run-smoke-tests.sh --seed --device <serial> <flow> || break; done`
8. Summarize:
   - root cause,
   - files changed,
   - repeat result,
   - whether the flow remains quarantined.
