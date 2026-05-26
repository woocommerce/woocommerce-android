#!/bin/bash -eu

# PR-time AI translation check.
#
# Translates only the keys this PR adds/changes (delta vs the committed
# translation-manifest.json) for every supported locale, then a bot commits the
# resulting values-*/strings.xml + manifest back to the PR branch. This keeps
# trunk fully translated (no post-merge step) and makes the translations
# reviewable inline in the PR diff (non-blocking spot-check).
#
# Idempotent: if the PR introduces no string changes (or they are already
# translated) the engine is a no-op, nothing is committed, and the bot commit
# carries `[skip ai-translate]` so the follow-up build does not loop.

BOT_SKIP_MARKER="[skip ai-translate]"

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

echo '--- :robot_face: Use bot for git operations'
source use-bot-for-git

echo '--- :ruby: Setup Ruby Tools'
install_gems

echo '--- :globe_with_meridians: AI translate (PR delta)'
bundle exec fastlane ai_translate mode:prtime

CHANGED=$(git status --porcelain -- 'WooCommerce/src/main/res/values-*/strings.xml' 'fastlane/ai_translation/translation-manifest.json')

if [ -z "${CHANGED}" ]; then
  echo "No translation changes — no commit, no retrigger."
  comment_on_pr --id ai-translations --if-exist delete || true
  exit 0
fi

echo '--- :memo: Commit translations back to the PR branch'
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
