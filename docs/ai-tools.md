# AI Dev and Testing Tools

Every AI tool, command and skill used for WooCommerce Android development and testing.
Adds or changes an agent, MCP server, testing helper or CI workflow? Update this file in the same PR.

**Availability:** `Repo` works after cloning. `MCP` needs the server to start (Node.js, plus a device or emulator for `mobile-mcp`, an Automattic login for `context-a8c`). Agents: before using an `MCP` tool, check that it is available and logged in. If it is not, tell the user what is missing and use the fallback listed, instead of failing silently.

## Skills

Skills live in `.agents/skills/` (`.claude/skills/` is a symlink). Agents discover them on their own and load one when the task matches. Each `SKILL.md` describes what the skill does and when to use it, so they are not listed here.

## Agents

| Name | What it does | When to use | Example | Owner | Availability |
|---|---|---|---|---|---|
| [`debugger`](../.claude/agents/debugger.md) | Diagnoses build, test and runtime failures | A build or test fails and the cause is unclear | "Use the debugger agent on this test failure" | Woo Mobile | Repo (Claude Code) |
| [`test-writer`](../.claude/agents/test-writer.md) | Writes unit tests following `docs/store-testing.md` or `docs/pos-testing.md` | Writing tests for a class, store app or POS | "Use the test-writer agent for OrderListViewModel" | Woo Mobile | Repo (Claude Code) |

## Testing helpers

| Name | What it does | When to use | Example | Owner | Availability |
|---|---|---|---|---|---|
| [`agent-login`](../.agents/skills/verify-on-device/references/agent-auto-login.md) (auto-login) | Logs a debug build in to a test store from a local profile, without typing credentials | Any manual or agent test that needs a logged-in app | `tools/agent-login/agent-login.sh --flavor dev --serial emulator-5554` | Woo Mobile | Repo. `installWasabiDebug` installs the `dev` flavor. Needs a profile in `~/.config/woocommerce-android/auto-login/profiles/` |
| [ApiFaker ADB commands](api-faker-adb.md) | Fakes API responses in a debug build through `adb` broadcasts | Testing error states and edge cases without a real backend | `adb shell am broadcast -p com.woocommerce.android.dev -a com.woocommerce.android.apifaker.SET_STATUS --ez enabled true` | Woo Mobile | Repo (debug builds) |
| POS direct launch | Opens POS directly, skipping the store app navigation | Checking POS on a tablet or emulator | `adb shell am start -n com.woocommerce.android.dev/com.woocommerce.android.ui.woopos.root.WooPosActivity` | Woo Mobile | Repo (debug builds) |

## MCP servers

Configured in [`.mcp.json`](../.mcp.json).

| Name | What it does | When to use | Example | Owner | Availability |
|---|---|---|---|---|---|
| `mobile-mcp` | Taps, swipes, screenshots and reads the screen of a device or emulator | Driving the app in an agent test | Used by `verify-on-device` | mobile-next (third party) | MCP. Fallback: `adb` |
| `context-a8c` | Reads Linear, Slack, P2 and GitHub context | Reading the Linear issue or a Slack thread behind a change | "Read WOOMOB-4291" | Automattic | MCP. Needs an Automattic login and proxy. Optional plugin with the same access as skills: `/plugin install context-a8c@automattic-claude-code-plugins`. Fallback: ask the user to paste the content |

## CI automation

| Name | What it does | When to use | Example | Owner | Availability |
|---|---|---|---|---|---|
| [AI Code Review](../.github/workflows/ai-code-review.yml) | Posts an AI review on a PR | Runs on PR open. Re-run on demand | Comment `@claude review` on the PR | Woo Mobile | CI |
| [Claude Review Recommendation](../.github/workflows/claude-review-recommendation.yml) | Labels a PR `review: recommended` or `review: optional` | Runs on every PR push | Automatic | Woo Mobile | CI |
| [Claude Build Analysis](../.buildkite/claude-analysis.yml) | Explains a failing Buildkite build in the build annotations and links them from a PR comment | Runs when a build fails | Automatic | Woo Mobile | CI |

## Configuration

| Name | What it does | When to use | Example | Owner | Availability |
|---|---|---|---|---|---|
| [`AGENTS.md`](../AGENTS.md) | Shared instructions for every agent. `CLAUDE.md` imports it | Changing a rule all agents must follow | Edit the file | Woo Mobile | Repo |
| [`.claude/settings.json`](../.claude/settings.json) | Claude Code permissions shared by the team | Allowing or denying a command for everyone | Add `Bash(adb *)` to `allow` | Woo Mobile | Repo (Claude Code) |
| [`.aiexclude`](../.aiexclude) | Files that AI tools in Android Studio must not read | Adding a new secret or generated file | Add a path pattern | Woo Mobile | Repo (Android Studio) |
