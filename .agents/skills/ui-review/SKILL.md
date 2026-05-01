---
name: ui-review
description: Generate screenshot coverage for screen-level Compose previews from the current diff or a specific target, check required visual and data variations, and produce a compact visual report
user-invocable: true
---

# UI Review

Run a visual review for the screens affected by the current diff or a specific target.

## Input Modes

This skill supports two starting modes:

- `Diff mode`: use the current diff to find the affected Compose code and trace it to the right screen previews
- `Target mode`: when the user explicitly provides a Compose name, class name, or file name, use that as the starting point instead of the diff

In `Target mode`:

- treat the provided Compose, class, or file as the source of truth for what to inspect
- trace from that target to the most relevant screen-level preview
- if the target is already a screen-level preview, use it directly
- if the target cannot be resolved to a usable screen or preview, stop and report that clearly instead of guessing
- if the target resolves to a non-Compose screen, Fragment, XML view, RecyclerView screen, or custom Android View screen, stop and report that this skill does not support that target
- do not convert, port, wrap, mirror, or recreate non-Compose UI in Compose just to make this skill run
- do not run screenshot generation when the resolved target is not Compose-based

## Kickoff

When the user explicitly invokes this skill, default to `Diff mode`.

At the start:

- if the user already gave a Compose name, class name, or file name, use `Target mode`
- otherwise, use `Diff mode`

## What This Skill Does

1. Chooses a starting point from the current diff or an explicit user-provided target
2. Traces that starting point up to screen-level composables
3. Prefers existing screen previews
4. Creates temporary previews only when coverage is missing
5. Deletes any previous `ui-review` report output
6. Runs the Android screenshot generation task
7. Builds a user-facing visual report
8. Deletes every temporary input and raw screenshot artifact

## Output Rules

The final output is a visual report at:

`WooCommerce/build/reports/ui-review/index.html`

The report may keep a small `assets/` folder next to it for images used by the report.

Keep the output contract fixed:

- `WooCommerce/build/reports/ui-review/index.html`
- `WooCommerce/build/reports/ui-review/assets/...`

These are the final artifacts and must remain after the run.

After the skill finishes, `git status` should not show screenshot junk.

Before starting a new run, delete any existing `WooCommerce/build/reports/ui-review` folder so the report is always rebuilt from a clean slate.

## Preview Usage

Because this skill runs Android's Compose Preview Screenshot Testing tasks, every screenshot preview must live in
`WooCommerce/src/screenshotTest/kotlin/...` and must be annotated with `@PreviewTest`.

This skill only supports Compose UI targets.

Supported targets:

- screen-level composables
- Compose previews
- Compose files that can be traced to a screen-level Compose preview

Unsupported targets:

- Fragments whose primary UI is XML or View-based
- custom Android Views
- RecyclerView screens that are not Compose
- screens that would require recreating a non-Compose UI in Compose

Do not modify preview annotations in the main source set for this workflow.
Keep the main project preview files unchanged and create screenshot-specific previews only in `UiReviewTemp.kt`.

Reuse existing screen preview coverage by recreating the same Compose screen and state in `UiReviewTemp.kt`.
If the original preview or its sample data is `private`, copy only the minimum required setup into the temp file.

Do not default to isolated component previews.
Only create a component-level preview when the changed UI cannot be seen clearly in any selected screen preview.

For screenshot previews in `UiReviewTemp.kt`, use explicit `@Preview(...)` definitions so each rendered image changes only one intended variable.

For each screenshot preview:

- use exactly one `@PreviewTest`
- create one preview function per final rendered variant
- use clear preview names such as `Light`, `Dark`, `Large Font`, `RTL`, and `Landscape`
- do not use shared multi-preview annotations from the project — always write a single explicit `@Preview(...)` per function

Use these exact `@Preview` definitions for each variant:

- `Light`: `@Preview(name = "Light", showBackground = true, uiMode = UI_MODE_NIGHT_NO)`
- `Dark`: `@Preview(name = "Dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)`
- `Large Font`: `@Preview(name = "Large Font", showBackground = true, fontScale = 2.0f, uiMode = UI_MODE_NIGHT_NO)`
- `RTL`: `@Preview(name = "RTL", showBackground = true, locale = "ar", uiMode = UI_MODE_NIGHT_NO)`
- `Landscape`: `@Preview(name = "Landscape", showBackground = true, device = Devices.PIXEL_9, widthDp = 640, heightDp = 360, uiMode = UI_MODE_NIGHT_NO)`

Treat `Data variation 1 - Long strings`, `Data variation 2 - Null data`, and any additional data variations as separate data previews.
Only create a data variation when the affected screen has visible inputs that make that variation meaningful.
For example:

- `Data variation 1 - Long strings` applies only when visible string content can be made longer
- `Data variation 2 - Null data` applies only when visible nullable inputs can be set to `null`

Do not mix them into every visual variation.

If the screen is Compose-based but has no usable preview at all, create a full temp screen preview for that screen.

## Required Visual Variations

Every affected screen should be checked in these visual conditions when practical:

- Light
- Dark
- Large font
- Landscape
- RTL

## Data Variations

Data variations are optional.
A screen may have zero data variations if none of its visible inputs support them.

When they exist, use these slots first:

- `Data variation 1 - Long strings`
- `Data variation 2 - Null data`

After those, add zero or more additional data variations only when the affected screen needs them:

- `Empty list`
- Other screen-specific data variations when they are justified by the changed UI

## Data Variation Rules

Rules:

- `Data variation 1 - Long strings`: make the visible strings longer in that preview only
- `Data variation 2 - Null data`: set the visible nullable fields to `null` in that preview only
- `Empty list`: use `emptyList()` in that preview only
- Additional data variations: add them only when the changed UI has a meaningful input or state worth isolating

For each `Data variation 2 - Null data` preview, keep a short list of which fields were set to `null`.

If the app already has a dedicated empty-screen composable or preview, treat it like any other screen and include it through normal coverage. Do not give it special handling just because it is an empty state.

## Temporary Preview Rules

Temporary previews are required for this workflow, and they must follow these rules:

- Put them under `WooCommerce/src/screenshotTest/kotlin/...`
- Use a stable temporary file name
- Annotate every screenshot preview with `@PreviewTest`
- Make every screenshot preview single-variation and explicit
- Do not stack annotations in a way that can change theme, direction, font scale, and orientation at the same time
- If the changed UI would be off-screen or too small to review in a required variation, adjust only the sample data or screen state so the changed area stays visible
- Reuse existing preview sample data whenever possible
- Only create previews for affected screens
- Only create temp previews for Compose-based affected screens
- Do not commit them
- Delete them before finishing

Use stable names:

- temporary preview file: `UiReviewTemp.kt`
- final report folder: `ui-review`
- final report file: `index.html`

## Screenshot Command

Generate screenshots with:

```bash
./gradlew --no-configuration-cache -Pandroid.experimental.enableScreenshotTest=true :WooCommerce:updateVanillaDebugScreenshotTest
```

Do not run this in the background.

## Report Template

Use the fixed HTML template at:

`$skill_path/assets/report-template.html`

Do not redesign the report on each run.
Only fill the placeholders in the template.

The hero section is fixed:

- show a summary badge at the top of the hero with the total number of issues found
- format the badge text as `0 issues found`, `1 issue found`, or `N issues found`
- use the title `UI Review`
- use the subtitle `Reviewed screens`, followed by only a bullet list of the reviewed screen names in the hero body

The report should:

- show issues first
- put the most important problem at the top
- label issue cards as `Issue 1`, `Issue 2`, `Issue 3`, and so on
- number issue cards in the same order they appear in the report
- do not add extra explanatory text outside the parts explicitly defined by these report rules
- include annotated screenshots only where needed
- stay visually clean and compact
- add text below a variant title only for issue cards and data variation cards

When you need text below a variant title, use the fixed template classes directly:

- wrap the line in `.meta`
- use `.meta-label` for the label
- use `.meta-value` for the value text

For `Data variation 1 - Long strings` cards in the report:

- do not add any text below the variant title

For `Data variation 2 - Null data` cards in the report:

- add one `.meta` line under the variant title
- list only the fields that were set to `null`
- example: `Null fields: customerName, billingAddress`

## Asset Selection Rules

Copy only the screenshots that will actually be shown in the final report into `ui-review/assets`.

If dark mode is part of the generated set, make sure the final report includes at least one dark screenshot.

The final report must use only the screenshots copied into `ui-review/assets`.
After those files are copied, the raw screenshot output folders can be deleted.

When variants are shown in the report, always keep this order:

First, show any issue variants.

After that, keep this order for the remaining variants:

1. Light
2. Dark
3. Large Font
4. RTL
5. Data variation 1 - Long strings
6. Data variation 2 - Null data
7. Additional data variations in an order that matches their importance to the changed UI
8. Landscape

If one of these variants does not exist for the current screen, skip it and keep the remaining order unchanged.

## Analysis Rules

Look for:

- wrapped or clipped tab labels
- text overflow
- overlap
- broken spacing
- unreadable contrast
- RTL mirroring problems
- landscape-specific awkwardness
- large-font breakage

Treat the following as issues when they are clearly visible in the rendered images:

- text is clipped, wrapped badly, or becomes hard to read
- UI elements overlap each other
- spacing breaks the layout in an obvious way
- important content such as buttons, titles, prices, or notices becomes hidden or hard to use
- large-font rendering breaks the layout
- landscape rendering becomes awkward or broken
- RTL rendering shows incorrect order, alignment, or direction
- light or dark theme rendering makes content hard to read
- long strings, null data, or empty lists expose a visible layout problem

These are common examples, not an exhaustive list.
Also report any other clearly visible visual regression that would likely matter to a user of the screen.

Only report issues that are clearly visible in the rendered images.
Do not report an issue against a required variation if the screenshot also contains an unintended second variation.

## Cleanup

Before finishing:

1. Delete temporary preview source files
2. Copy the screenshots needed by the report into `WooCommerce/build/reports/ui-review/assets`
3. Delete `WooCommerce/src/screenshotTestVanillaDebug`
4. Delete `WooCommerce/build/outputs/screenshotTest-results`
5. Delete `WooCommerce/build/reports/screenshotTest`
6. Delete tool-generated screenshot test reports under `WooCommerce/build/reports/tests` for this run
