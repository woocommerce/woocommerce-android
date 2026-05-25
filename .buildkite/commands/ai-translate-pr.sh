#!/bin/bash -eu

# PR-time AI translation check.
#
# Translates only the keys this PR adds/changes (delta vs the PR base
# strings.xml) for every supported locale, then a bot commits the
# resulting values-*/strings.xml + manifest back to the PR branch. This keeps
# trunk fully translated (no post-merge step) and makes the translations
# reviewable inline in the PR diff (non-blocking spot-check).
#
# Idempotent: if the PR introduces no string changes (or they are already
# translated) the engine is a no-op, nothing is committed, and the bot commit
# carries `[skip ai-translate]` so the follow-up build does not loop.

BOT_SKIP_MARKER="[skip ai-translate]"
MAIN_STRINGS="WooCommerce/src/main/res/values/strings.xml"

changed_source_keys() {
  local base_branch="${BUILDKITE_PULL_REQUEST_BASE_BRANCH:-trunk}"
  local base_file
  base_file="$(mktemp)"

  git fetch --no-tags origin "${base_branch}:refs/remotes/origin/${base_branch}"
  git show "origin/${base_branch}:${MAIN_STRINGS}" > "${base_file}"

  ruby -I fastlane/ai_translation/lib -r woo_ai_translation - "${base_file}" "${MAIN_STRINGS}" <<'RUBY'
old_path, new_path = ARGV

def translatable_signatures(path)
  WooAiTranslation::AndroidResources::Parser.parse_file(path).translatable_units.to_h do |unit|
    [unit.name, unit.source_signature]
  end
end

old_units = translatable_signatures(old_path)
new_units = translatable_signatures(new_path)

changed_or_added = new_units.select { |name, signature| old_units[name] != signature }.keys
deleted = old_units.keys - new_units.keys

puts (changed_or_added + deleted).join(',')
RUBY

  rm -f "${base_file}"
}

comment_failure() {
  local message="$1"
  comment_on_pr --id ai-translations "## :globe_with_meridians: AI translations did not update

${message}

The Buildkite step is currently soft-failed, so check the AI Translations job log before relying on this PR's localization diff." || true
}

# Fork PRs / no-secret builds: no API key is injected. Skip cleanly; the
# code-freeze sweep is the safety net for these deltas.
if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
  echo "ANTHROPIC_API_KEY not available (fork PR or no secret) — skipping AI translation."
  exit 0
fi

# Don't re-process our own bot commit.
if git log -1 --pretty=%B | grep -qF "${BOT_SKIP_MARKER}"; then
  echo "HEAD is a bot translation commit — nothing to do."
  exit 0
fi

echo '--- :mag: Detect changed source strings'
CHANGED_KEYS="$(changed_source_keys)"

if [ -z "${CHANGED_KEYS}" ]; then
  echo "No changed translatable source strings — no AI translation needed."
  comment_on_pr --id ai-translations --if-exist delete || true
  exit 0
fi

echo "Changed translatable source keys: ${CHANGED_KEYS}"

echo '--- :ruby: Setup Ruby Tools'
install_gems

echo '--- :globe_with_meridians: AI translate (PR delta)'
set +e
bundle exec fastlane ai_translate mode:prtime only_keys:"${CHANGED_KEYS}" strict:true
TRANSLATE_EXIT=$?
set -e

if [ "${TRANSLATE_EXIT}" -ne 0 ]; then
  comment_failure "The translation engine exited with status ${TRANSLATE_EXIT} while processing:
\`${CHANGED_KEYS}\`"
  exit "${TRANSLATE_EXIT}"
fi

CHANGED=$(git status --porcelain -- 'WooCommerce/src/main/res/values-*/strings.xml' 'fastlane/ai_translation/translation-manifest.json')

if [ -z "${CHANGED}" ]; then
  comment_failure "The job found changed source strings but the translation engine did not produce any file changes:
\`${CHANGED_KEYS}\`"
  exit 1
fi

echo '--- :memo: Commit translations back to the PR branch'
echo '--- :robot_face: Use bot for git operations'
set +u
source use-bot-for-git
set -u

git add -- 'WooCommerce/src/main/res/values-*/strings.xml'
if [ -f fastlane/ai_translation/translation-manifest.json ]; then
  git add -- 'fastlane/ai_translation/translation-manifest.json'
fi
git commit -m "Update AI translations ${BOT_SKIP_MARKER}"
git push origin "HEAD:${BUILDKITE_BRANCH}"

# Non-blocking spot-check surface (decoupled from Danger; equally visible).
comment_on_pr --id ai-translations "## :globe_with_meridians: AI translations updated

This PR's new/changed strings were machine-translated for all supported
locales and committed by the bot. Review is **sampled and non-blocking** —
spot-check the \`values-*/strings.xml\` diff if a string is nuanced.

Updated:
\`\`\`
${CHANGED}
\`\`\`"
