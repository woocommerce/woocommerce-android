# Implementation Spec: Eliminate the Translation Rebuild

This document describes the specific changes needed to integrate AI translations into the WooCommerce Android release pipeline and eliminate the extra build that currently exists only because translations arrive late and the version name changes.

## Goal

Remove the guaranteed second build from each release by:
1. Translating strings with AI at code freeze time (before the first build)
2. Using the final version name from the start (no `-rc-N` → `X.Y` rename)

## Changes

### 1. New: AI translation script

Create `scripts/ai-translations/integrate/translate.py`

This Python script translates new/changed strings by calling the Anthropic (or OpenAI) API.

Flow:
- Find previous release tag: `git describe --tags --abbrev=0 --match="*.*"`
- Extract strings from previous and current `WooCommerce/src/main/res/values/strings.xml`
- Diff to find new keys and changed values
- Build a naive translation prompt (experiment showed code context doesn't help):
  ```
  Translate the following Android string resources from English to {LANGUAGE}.
  Return a JSON array of objects with "key" and "value" fields.
  Keep all format placeholders like %1$s, %1$d exactly as they are.
  Keep all HTML tags like <b>, </b> exactly as they are.
  ```
- Call the LLM API for all 16 languages (parallel, one call per language)
- Output per-language JSON files to `/tmp/ai-translations/`

Notes:
- The release-toolkit gem has `LocalizeHelper` with existing XML parsing and merging logic (used by `android_download_translations`). This could be reused for reading/writing `strings.xml` instead of writing new parsing code. The experiment branch has `parse_strings.py` as an alternative.
- API call is a standard HTTP request. `ANTHROPIC_API_KEY` is already in Buildkite secrets. If using OpenAI, add `OPENAI_API_KEY`.
- A typical release adds 20-50 strings. This fits in a single API call per language, no chunking needed. Total time: ~2 minutes.
- venv is required on macOS (system Python blocks global pip installs).

### 2. New: Translation merge + validation

Create `scripts/ai-translations/integrate/merge_translations.py` (or reuse `LocalizeHelper` from the release-toolkit gem)

For each language JSON file:
- Validate: all format placeholders (`%1$s`, `%d`) from English are present, XML is well-formed
- Skip invalid translations and keep existing ones
- Merge into `WooCommerce/src/main/res/values-{locale}/strings.xml`
- Handle locale mapping: `pt-br` → `values-pt-rBR`, `zh-cn` → `values-zh-rCN`
- Handle legacy dirs: write to both `values-he` and `values-iw`, `values-id` and `values-in`

### 3. New: Fastlane lane

Add to `fastlane/Fastfile` after `download_translations` lane (~line 475):

```ruby
lane :ai_translate_strings do |skip_confirm: false|
  ensure_git_status_clean
  ensure_git_branch_is_release_branch!
  configure_apply(force: is_ci)

  # Setup venv, run translate.py, run merge, commit, push
end
```

Must accept `skip_confirm`, call `configure_apply` (sets up secrets), commit and push.

### 4. New: Buildkite pipeline

Create `.buildkite/release-pipelines/ai-translate-strings.yml`

Same pattern as `download-release-translations.yml`:
```yaml
steps:
  - label: "AI Translate Strings"
    plugins: [$CI_TOOLKIT]
    command: |
      echo '--- :robot_face: Use bot for git operations'
      source use-bot-for-git
      .buildkite/commands/checkout-release-branch.sh "$RELEASE_VERSION"
      echo '--- :ruby: Setup Ruby Tools'
      install_gems
      echo '--- :globe_with_meridians: AI Translate Strings'
      bundle exec fastlane ai_translate_strings skip_confirm:true
    agents:
      queue: "mac-metal"
    retry:
      manual:
        reason: "Re-trigger from Releases V2"
        allowed: false
```

### 5. Modify: `start_code_freeze` (Fastfile line 132)

Current (line 141):
```ruby
new_beta_version = beta_version_next(version_name: new_version)
```
Sets version name to `X.Y-rc-1`.

Change: set version name to `X.Y` directly and bump version code. Do not call `beta_version_next`.

### 6. Modify: `build_and_upload_google_play` (Fastfile line 648)

Current (line 650):
```ruby
if beta_version?(version_name_current)
```
Checks for `-rc-` in version name. Beta → `beta` track, non-beta → `production` track.

Change: pass an explicit `track` parameter instead of inferring from version name. Add `TRACK` to the environment variables in `trigger_release_build` (line 777):
```ruby
environment = {
  INCLUDE_WEAR_APP: include_wear_app,
  RELEASE_VERSION: release_version_current,
  TRACK: track  # "beta" or "production"
}
```
Then read `ENV['TRACK']` in `build_and_upload_google_play` instead of calling `beta_version?`.

### 7. Modify: `new_beta_release` (Fastfile line 316)

Current (line 338):
```ruby
VERSION_FILE.write_version(
  version_name: beta_version_next,
  version_code: build_code_next
)
```
`beta_version_next` (line 1638) uses `RCNotationVersionFormatter` to parse `X.Y-rc-1` → `X.Y-rc-2`.

Change: keep version name, only increment version code:
```ruby
VERSION_FILE.write_version(
  version_name: release_version_current,
  version_code: build_code_next
)
```

### 8. Modify: `finalize_release` (Fastfile line 519)

Currently does:
- Line 532: writes version name (`X.Y`) and bumps build code
- Line 547: triggers build via `trigger_release_build`
- Line 549: creates backmerge PR
- Line 551: removes branch protection
- Line 563: closes milestone

Change: split into two lanes:
- **`promote_to_production`** (new) - promotes existing build from beta to production track in Play Store
- **`finalize_release`** (modified) - keeps only: backmerge PR, remove branch protection, close milestone. No version change, no build trigger.

### 9. Modify: `beta_version?` (Fastfile line 1538)

Current:
```ruby
def beta_version?(version)
  version.include? '-rc-'
end
```

Change: remove or replace. With explicit track routing (change #6), this is no longer needed for track decisions. Audit other usages in the Fastfile.

### 10. Modify: release-toolkit gem

`fastlane-plugin-wpmreleasetoolkit` (github.com/wordpress-mobile/release-toolkit):
- `RCNotationVersionFormatter` - not used for WCAndroid anymore
- `beta_version?` / `beta_version_next` - need alternative for non-RC products

Changes should be additive (support both RC and non-RC) since other apps use the same gem.

### 11. Modify: Releases V2 scenario

File: `wcandroid.php` in the Releases V2 config

| Milestone | Current Button | Change |
|-----------|---------------|--------|
| Code Freeze | "Start Code Freeze" (line 48) | Update description: version is `X.Y` not `X.Y-rc-1` |
| Code Freeze | (none) | **Add** "AI Translate Strings" button before "Complete Code Freeze". Triggers `ai-translate-strings.yml` |
| Code Freeze | "Complete Code Freeze" (line 103) | Keep as-is |
| Intermediate Beta | "New Beta Release" (line 172) | Update description: only bumps version code |
| Play Store Submission | "Download Translations" (line 236) | **Remove** |
| Play Store Submission | "Finalize Release" (line 255) | **Replace** with "Promote to Production" + "Finalize (admin)" |
| Release | "Publish GitHub Release" (line 341) | Keep as-is |

Update Slack messages (lines 156-159, 203-209, 366-373) to remove `rc-1` references.
