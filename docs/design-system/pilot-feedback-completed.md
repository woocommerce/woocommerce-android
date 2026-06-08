# Pilot: Feedback Completed

`FeedbackCompletedFragment` is the first XML/View pilot for Store Management App design-system integration.

The goal is to validate the adapter, the Fragment-hosted Compose migration workflow, and the documentation future AI agents should follow.

## Target Files

- `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/feedback/FeedbackCompletedFragment.kt`
- `WooCommerce/src/main/res/layout/fragment_feedback_completed.xml`
- `WooCommerce/src/main/res/navigation/nav_graph_main.xml`
- `WooCommerce/src/main/res/navigation/nav_graph_settings.xml`

## Why This Screen

- It is visible but low-risk.
- It is not a product, order, payment, fulfillment, or POS workflow.
- It has bounded navigation and no ViewModel.
- It exercises design-system basics: top bar, title text, body text, link text, image, primary button, spacing, and scroll behavior.
- It can become the first concrete AI-agent migration example.

## Behavior To Preserve

- `activityAppBarStatus` remains hidden because the screen owns its top bar.
- Close navigation uses `findNavController().navigateUp()`.
- The "Back to store" button keeps the current back behavior.
- The inline "contact us" text opens help with `HelpOrigin.FEEDBACK_SURVEY`.
- `AnalyticsTracker.trackViewShown(this)` still runs on resume.
- `AnalyticsEvent.SURVEY_SCREEN` still tracks `KEY_FEEDBACK_CONTEXT` and `VALUE_FEEDBACK_COMPLETED`.
- Existing string resources and drawable assets remain unless a design decision changes them.
- Existing nav graph destinations and SafeArgs remain.

## Design-System Coverage

The pilot should consume production-ready design-system APIs for:

- `WooTheme.*` foundations under both `WooThemeWithBackground` and
  `WooDesignSystemThemeWithBackground`.
- Top bar or navigation bar component.
- Page title/body/link typography.
- Primary button.
- Spacing/padding tokens.
- Radius/color tokens required by those components.
- Image/content layout conventions.

If a required component is not production-ready yet, either implement it first or document why the pilot temporarily uses a legacy component.

## Preview Coverage

Add previews for:

- Legacy-compatible foundation in light and dark mode.
- Design-system foundation in light and dark mode.
- Long body/link text if layout can wrap.
- Large font scale if text or button layout is sensitive.

## Verification

Before treating the pilot as the migration template:

- Compare the migrated screen against the XML baseline.
- Verify no accessibility regression from the XML baseline.
- Verify close navigation.
- Verify "Back to store" behavior.
- Verify the inline help link.
- Verify analytics are unchanged.
- Verify the same screen implementation renders with both foundation states.
- Verify DS components under `WooThemeWithBackground` do not fall back to `LightWooColors`.
- Verify `WooTopAppBar` legacy-compatible behavior is close enough to the Activity toolbar when the
  flag is off.
- Review light and dark previews/screenshots.
- Confirm no POS APIs or patterns were introduced.

## Findings After Implementation

Add findings here only when the pilot reveals something worth preserving for future migrations, such as a component gap, token mismatch, verification issue, or playbook change. Do not add trivial implementation notes.
