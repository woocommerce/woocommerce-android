# create-pr

Create a draft pull request. Do not use Claude anywhere. As template for description use PULL_REQUEST_TEMPLATE.md and for the title use previously created by me PRs.

## Rules
- Create PR as DRAFT
- Never mention Claude, AI, or any AI tools in PR title, description, or commits
- Never use Co-Authored-By in commits
- Use simple, plain language
- Do not mention that any CI tools like linters, detekt and so on are used
- Do not mention that any tests are run or that the code is tested automatically

## Title Format
Look at previous PRs for format. Examples:
- [WOOMOB-1022] Add settings infrastructure for Woo POS
- [WOOMOB-971] Fix first order not auto-selecting on tablet when navigating from dashboard
- [Woo POS][Settings] Implement settings screen navigation

## Description Template
Use the template from .github/PULL_REQUEST_TEMPLATE.md, filling in:
- Issue number (e.g., WOOMOB-1022)
- Description of what was changed
- Steps to reproduce/test
- Testing information
- Tests performed
- Screenshots if UI changes

Remove template comments and keep it concise.
