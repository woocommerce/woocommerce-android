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
SKIP_LABEL="skip-ai-translation"
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

added_source_keys() {
  local base_branch="${BUILDKITE_PULL_REQUEST_BASE_BRANCH:-trunk}"
  local base_file
  base_file="$(mktemp)"

  git fetch --no-tags origin "${base_branch}:refs/remotes/origin/${base_branch}"
  git show "origin/${base_branch}:${MAIN_STRINGS}" > "${base_file}"

  ruby -I fastlane/ai_translation/lib -r woo_ai_translation - "${base_file}" "${MAIN_STRINGS}" <<'RUBY'
old_path, new_path = ARGV

def translatable_names(path)
  WooAiTranslation::AndroidResources::Parser.parse_file(path).translatable_units.map(&:name)
end

old_names = translatable_names(old_path)
new_names = translatable_names(new_path)

puts (new_names - old_names).join(',')
RUBY

  rm -f "${base_file}"
}

apply_generated_context_comments() {
  local context_json="$1"

  ruby -rjson - "${MAIN_STRINGS}" "${context_json}" <<'RUBY'
strings_path, context_json = ARGV

data = JSON.parse(File.read(context_json))
contexts = {}

def safe_xml_comment(text)
  description = text.to_s.gsub(/\s+/, ' ').strip
  description.gsub!('--', '- -') while description.include?('--')
  return nil if description.empty? || description.end_with?('-') || description.include?('--')

  description
end

data.fetch('entries', []).each do |entry|
  next unless entry['error'].to_s.empty?

  key = entry['key'].to_s.sub(/:[a-z]+\z/, '').sub(/\[\d+\]\z/, '')
  description = safe_xml_comment(entry.dig('context', 'description'))
  next if key.empty? || description.empty?

  contexts[key] ||= description
end

exit 0 if contexts.empty?

lines = File.readlines(strings_path, encoding: 'UTF-8')
output = []
changed = false

lines.each do |line|
  if (match = line.match(/^(\s*)<(?:string|string-array|plurals)\s+name="([^"]+)"/))
    indent = match[1]
    key = match[2]
    description = contexts[key]

    if description && !output.last.to_s.match?(/^\s*<!--\s*#{Regexp.escape(description)}\s*-->\s*$/)
      output << "#{indent}<!-- #{description} -->\n"
      changed = true
    end
  end

  output << line
end

if changed
  File.write(strings_path, output.join)
  puts "Added generated context comments for #{contexts.keys.size} string(s)."
end
RUBY
}

add_context_for_new_source_keys() {
  local new_keys="$1"
  [ -n "${new_keys}" ] || return 0

  local context_json
  context_json="$(mktemp)"

  echo '--- :memo: Generate context for new source strings'
  echo "New translatable source keys: ${new_keys}"

  set +e
  bundle exec i18n-context-generator extract \
    --translations "${MAIN_STRINGS}" \
    --source "WooCommerce/src/main" \
    --keys "${new_keys}" \
    --format json \
    --output "${context_json}" \
    --provider anthropic \
    --model claude-sonnet-4-6
  local context_exit=$?
  set -e

  if [ "${context_exit}" -ne 0 ]; then
    echo "i18n-context-generator exited with status ${context_exit}; continuing without generated context."
    rm -f "${context_json}"
    return 0
  fi

  if ! apply_generated_context_comments "${context_json}"; then
    echo "Could not apply generated context comments; continuing without generated context."
  fi

  rm -f "${context_json}"
}

comment_failure() {
  local message="$1"
  comment_on_pr --id ai-translations "## :globe_with_meridians: AI translations did not update

${message}

Check the AI Translations job log before relying on this PR's localization diff." || true
}

github_repo() {
  git config --get remote.origin.url |
    sed -E 's#^git@github.com:##; s#^https://github.com/##; s#\.git$##'
}

github_token() {
  if [ -n "${GITHUB_TOKEN:-}" ]; then
    echo "${GITHUB_TOKEN}"
  elif [ -n "${AUTOMATTIC_GITHUB_TOKEN_READ_ONLY:-}" ]; then
    echo "${AUTOMATTIC_GITHUB_TOKEN_READ_ONLY}"
  fi
}

pr_has_skip_label() {
  local pr="${BUILDKITE_PULL_REQUEST:-}"
  [ -n "${pr}" ] && [ "${pr}" != "false" ] || return 1

  local token
  token="$(github_token)"
  if [ -z "${token}" ]; then
    echo "No GitHub token available to check for ${SKIP_LABEL}; continuing without bypass."
    return 1
  fi

  local repo
  repo="$(github_repo)"
  curl -fsSL \
    -H "Authorization: Bearer ${token}" \
    -H "Accept: application/vnd.github+json" \
    "https://api.github.com/repos/${repo}/issues/${pr}/labels" |
    ruby -rjson -e 'labels = JSON.parse($stdin.read).map { |l| l.fetch("name") }; exit(labels.include?(ARGV.fetch(0)) ? 0 : 1)' "${SKIP_LABEL}" 2>/dev/null
}

# Don't re-process our own bot commit.
if git log -1 --pretty=%B | grep -qF "${BOT_SKIP_MARKER}"; then
  echo "HEAD is a bot translation commit — nothing to do."
  exit 0
fi

if pr_has_skip_label; then
  echo "PR has ${SKIP_LABEL}; skipping AI translation."
  comment_on_pr --id ai-translations "## :globe_with_meridians: AI translations skipped

This PR is labeled \`${SKIP_LABEL}\`, so the required AI translation job was bypassed intentionally." || true
  exit 0
fi

# Fork PRs / no-secret builds: no API key is injected. Skip cleanly; the
# code-freeze sweep is the safety net for these deltas.
if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
  echo "ANTHROPIC_API_KEY not available (fork PR or no secret) — skipping AI translation."
  exit 0
fi

echo '--- :mag: Detect changed source strings'
CHANGED_KEYS="$(changed_source_keys)"
NEW_KEYS="$(added_source_keys)"

if [ -z "${CHANGED_KEYS}" ]; then
  echo "No changed translatable source strings — no AI translation needed."
  comment_on_pr --id ai-translations --if-exist delete || true
  exit 0
fi

echo "Changed translatable source keys: ${CHANGED_KEYS}"

echo '--- :ruby: Setup Ruby Tools'
install_gems

add_context_for_new_source_keys "${NEW_KEYS}"

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

TRANSLATION_CHANGED=$(git status --porcelain -- 'WooCommerce/src/main/res/values-*/strings.xml' 'fastlane/ai_translation/translation-manifest.json')

if [ -z "${TRANSLATION_CHANGED}" ]; then
  comment_failure "The job found changed source strings but the translation engine did not produce any file changes:
\`${CHANGED_KEYS}\`"
  exit 1
fi

CHANGED=$(git status --porcelain -- "${MAIN_STRINGS}" 'WooCommerce/src/main/res/values-*/strings.xml' 'fastlane/ai_translation/translation-manifest.json')

echo '--- :memo: Commit translations back to the PR branch'
echo '--- :robot_face: Use bot for git operations'
set +u
source use-bot-for-git
set -u

git add -- "${MAIN_STRINGS}"
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
