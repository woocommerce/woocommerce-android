---
name: maestro-flow-doctor
description: Repair a failing WooCommerce Android Maestro flow by reproducing it against the lab store, inspecting Maestro selectors, patching the smallest selector/wait/setup issue, and rerunning with repeat evidence. Human-triggered only.
allowed-tools: Bash, Read, Edit, Grep, Glob
user-invocable: true
---

# Maestro Flow Doctor

Use this skill only for repairing existing Maestro smoke flows under `.maestro/flows/`.

## Ground Rules

- Always run against the lab store: pass `--store lab`.
- Never run destructive repair loops against the shared store.
- Never ask the user to paste credentials. Validate that `.maestro/.env.local` has the required variable names without echoing values.
- Use Maestro MCP as the selector source of truth: `run` executes the YAML we ship, and `inspect_screen` shows the hierarchy Maestro selectors see.
- Keep fixes minimal: selector, wait, setup, or fixture query changes only. Do not broaden coverage while repairing a flake.
- Promotion still requires burst evidence; a local `--repeat` pass is repair evidence, not promotion.

## Workflow

1. Confirm the target flow path exists under `.maestro/flows/`.
2. Run syntax and coverage checks:
   - `.maestro/scripts/check-smoke-coverage.py`
   - `maestro test --dry-run <flow>` if supported by the installed Maestro version; otherwise continue with a real lab run.
3. Reproduce on a lab-store device:
   - `.maestro/scripts/run-smoke-tests.sh --store lab --include-tags flaky_quarantine <flow>`
4. At the failure point, use Maestro MCP `inspect_screen`.
5. Compare the failing selector with the hierarchy:
   - Prefer `id:` selectors exposed through `testTag`.
   - Use generated `strings.env` values for text assertions.
   - Do not add `point:` selectors unless the flow comment explains why no semantic selector exists.
6. Patch the smallest file set.
7. Rerun the single flow with repeat evidence:
   - `.maestro/scripts/run-smoke-tests.sh --store lab --repeat 3 <flow>`
8. Summarize:
   - root cause,
   - files changed,
   - repeat result,
   - whether the flow remains quarantined.
