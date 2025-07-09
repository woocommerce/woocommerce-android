# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands
- Build debug: `./gradlew assembleVanillaDebug`
- Install debug: `./gradlew installVanillaDebug`
- Run unit tests: `./gradlew testJalapenoDebug`
- Run single unit test: `./gradlew :WooCommerce:testJalapenoDebug --tests "com.woocommerce.android.path.to.TestClass"`
- Run code quality check: `./gradlew detektAll`

## Code Style Guidelines
- Never add comments
- Based on Android Code Style Guidelines with 120 character line length
- Use Kotlin for all new code with auto-formatting using Detekt
- Always use kotlinOptions `allWarningsAsErrors = true`
- Use Jetpack Compose for new UI components
- Naming conventions:
  - Layouts: `feature_name_layout_type` (e.g., `feedback_survey_activity`)
  - View IDs: `goalViewType` (e.g., `doneButton`, `headingTextView`)
  - Test names: `given X, when Y, then Z` format
- Add FIXME only locally; use TODO for committed code
- Set up EditorConfig with the project's style rules
- Ensure all Kotlin imports are explicit (no wildcard imports)
- Do not use comments. Only //GIVEN // WHEN //THEN for the test code
- Use assertJ for assertions in tests, not JUnit assertions
- Have an empty line at the end of each file


## Pull Request Guidelines
- Never add claude mention in the code or commits or PRs
- When create a PR do not keep the comments in the PR description
- Never add "Co-Authored-By" in the commit message
- Follow the format from my previous PRs when creating a new PR
- Always create draft PR
- Keep the style of the text simple in the PR description
