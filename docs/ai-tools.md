# AI Dev and Testing Tools

Every AI tool, command and skill used for WooCommerce Android development and testing.
Adds or changes a tool or command? Update this file in the same PR.

**Availability:** `Repo` works after cloning. `MCP` needs the server to start (Node.js, plus a device or emulator for `mobile-mcp`). `External` must be installed separately and needs a login. Agents: before using an `MCP` or `External` tool, check that it is available and logged in. If it is not, tell the user what is missing and use the fallback listed, instead of failing silently.

## Development skills

Skills live in `.agents/skills/` (`.claude/skills/` is a symlink). Agents load them on their own when the task matches.

| Name | What it does | When to use | Example | Owner | Availability |
|---|---|---|---|---|---|
| [`store-compose`](../.agents/skills/store-compose/SKILL.md) | Compose UI patterns for the store app | Compose UI outside `ui/woopos/` | "Add a Compose screen for order notes" | Woo Mobile | Repo |
| [`store-viewmodel`](../.agents/skills/store-viewmodel/SKILL.md) | `ScopedViewModel` patterns, events, navArgs | ViewModels outside `ui/woopos/` | "Add a loading state to the product list ViewModel" | Woo Mobile | Repo |
| [`store-analytics`](../.agents/skills/store-analytics/SKILL.md) | `AnalyticsEvent` tracking patterns | Tracking in the store app | "Track taps on the new button" | Woo Mobile | Repo |
| [`store-tests`](../.agents/skills/store-tests/SKILL.md) | Unit test patterns for the store app | Tests outside `ui/woopos/` | "Write tests for this ViewModel" | Woo Mobile | Repo |
| [`pos`](../.agents/skills/pos/SKILL.md) | POS architecture and design system | Any code in `ui/woopos/` | "Add a button to the POS cart" | Woo Mobile | Repo |
| [`pos-analytics`](../.agents/skills/pos-analytics/SKILL.md) | `WooPosAnalyticsEvent` tracking patterns | Tracking in POS | "Track POS checkout errors" | Woo Mobile | Repo |
| [`pos-tests`](../.agents/skills/pos-tests/SKILL.md) | Unit test patterns for POS | Tests in `ui/woopos/` | "Write tests for the POS totals ViewModel" | Woo Mobile | Repo |

## Review and PR skills

| Name | What it does | When to use | Example | Owner | Availability |
|---|---|---|---|---|---|
| [`review`](../.agents/skills/review/SKILL.md) | Reviews the branch diff against project rules | Before opening a PR | `/review` | Woo Mobile | Repo |
| [`pr`](../.agents/skills/pr/SKILL.md) | Creates a PR from the repo template | Opening a PR | "Create a PR" | Woo Mobile | Repo |
| [`pr-feedback`](../.agents/skills/pr-feedback/SKILL.md) | Evaluates review comments, fixes them after approval, replies | Addressing review comments | `/pr-feedback 16664` | Woo Mobile | Repo |

## Verification skills

| Name | What it does | When to use | Example | Owner | Availability |
|---|---|---|---|---|---|
| [`verify-on-device`](../.agents/skills/verify-on-device/SKILL.md) | Builds, installs, logs in and checks the app on an emulator or device | Checking a change in the running app | `/verify-on-device the new order filter` | Woo Mobile | Repo. Uses `mobile-mcp` or the Android CLI for agents, falls back to `adb` |
| [`ui-review`](../.agents/skills/ui-review/SKILL.md) | Renders screenshots of Compose previews touched by the diff and checks variations | Visual check of Compose changes without a device | `/ui-review` | Woo Mobile | Repo |
| [`woo-ai-smoke`](../.agents/skills/woo-ai-smoke/SKILL.md) | Runs the headless AI Assistant smoke suite | Changes to the AI Assistant | `/woo-ai-smoke` | Woo Mobile | Repo. Live mode needs a test store in `~/.woo-ai-smoke/store.env` |

## Agents

| Name | What it does | When to use | Example | Owner | Availability |
|---|---|---|---|---|---|
| [`debugger`](../.claude/agents/debugger.md) | Diagnoses build, test and runtime failures | A build or test fails and the cause is unclear | "Use the debugger agent on this test failure" | Woo Mobile | Repo (Claude Code) |
| [`test-writer`](../.claude/agents/test-writer.md) | Writes unit tests for the store app | Tests outside `ui/woopos/`. For POS use `pos-tests` | "Use the test-writer agent for OrderListViewModel" | Woo Mobile | Repo (Claude Code) |

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
| `context-a8c` | Reads Linear, Slack, P2 and GitHub context | Reading the Linear issue or a Slack thread behind a change | "Read WOOMOB-4291" | Automattic | MCP + External. Needs an Automattic login and proxy. Fallback: ask the user to paste the content |

## External plugins

| Name | What it does | When to use | Example | Owner | Availability |
|---|---|---|---|---|---|
| `context-a8c` | Skills for Linear, Slack, P2s and Automattic-wide search | Same as the MCP server, as skills | `/plugin install context-a8c@automattic-claude-code-plugins` | Automattic | External. The marketplace is on Automattic GitHub Enterprise. Needs an Automattic login |

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
