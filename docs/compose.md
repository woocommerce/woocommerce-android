# Jetpack Compose Guidelines

> **Scope:** Store management app (main app). POS has its own Compose patterns — see [POS Architecture](pos-architecture.md).

## Content

1. [Fragment Hosting](#fragment-hosting)
2. [Screen Composable Pattern](#screen-composable-pattern)
3. [Code Style](#code-style)
4. [Composable Function Rules](#composable-function-rules)
5. [Theming and Styling](#theming-and-styling)
6. [Existing Components](#existing-components)
7. [Previews](#previews)
8. [File Structure](#file-structure)
9. [Managing State](#managing-state)
10. [Navigation](#navigation)
11. [Accessibility](#accessibility)
12. [UI Tests in Compose](#ui-tests-in-compose)

## Fragment Hosting

Compose screens live inside Fragments in a 1:1 relationship. Use the `composeView {}` extension (from `com.woocommerce.android.ui.compose.composeView`) which handles `DisposeOnViewTreeLifecycleDestroyed` and `WooThemeWithBackground` automatically:

```kotlin
@AndroidEntryPoint
class MyFeatureFragment : BaseFragment() {
    private val viewModel: MyFeatureViewModel by viewModels()

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            MyFeatureScreen(
                viewModel = viewModel,
                onBack = { findNavController().popBackStack() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
            }
        }
    }
}
```

Key points:
- Set `activityAppBarStatus = AppBarStatus.Hidden` when the screen has its own Compose `Toolbar`
- Handle navigation events (`MultiLiveEvent`) in `onViewCreated`, not in composables
- Do NOT create XML layouts — use `composeView {}` directly

### Why `composeView {}`?

Compose views register with external event sources and can cause memory leaks if not disposed properly. The `composeView {}` helper sets `DisposeOnViewTreeLifecycleDestroyed` and wraps content in `WooThemeWithBackground`, so you don't have to do it manually.

If you need to write the boilerplate yourself (rare), use `ComposeView` with the correct strategy:

```kotlin
override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    return ComposeView(requireContext()).apply {
        setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WooThemeWithBackground {
                // Compose code here.
            }
        }
    }
}
```

## Screen Composable Pattern

Use two overloads: one VM-aware (called from Fragment) and one stateless (for previews and tests):

```kotlin
// VM-aware overload — extracts state from ViewModel
@Composable
fun MyFeatureScreen(
    viewModel: MyFeatureViewModel,
    onBack: () -> Unit,
) {
    val viewState by viewModel.state.observeAsState()
    viewState?.let {
        MyFeatureScreen(viewState = it, onBack = onBack)
    }
}

// Stateless overload — pure UI, used by previews
@Composable
fun MyFeatureScreen(
    viewState: MyFeatureViewState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Toolbar(
                title = viewState.title,
                onNavigationButtonClick = onBack,
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // content
        }
    }
}
```

State observation patterns (both are used in the codebase):
- `LiveData`: `val state by viewModel.viewState.observeAsState()`
- `StateFlow`: `val state by viewModel.uiState.collectAsState()`

## Code Style

We follow the official [Compose API guidelines for App development](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#api-guidelines-for-jetpack-compose). Many rules are enforced by Lint.

Project-specific exceptions:
- Use `UPPER_SNAKE_CASE` for constants (overrides the Compose guideline suggesting `PascalCase`)

Highlights from the official guidelines:
- `@Composable` functions returning Unit use `PascalCase` noun names (not verbs)
- `@Composable` functions returning values follow standard Kotlin naming conventions
- Functions that `remember {}` and return a mutable object should be prefixed with `remember`
- `@Composable` functions should either emit content or return a value, but not both

## Composable Function Rules

1. **Naming:** PascalCase noun names for `@Composable` functions returning Unit
2. **Modifier:** MUST accept `Modifier` as first optional parameter, named `modifier`:
   ```kotlin
   @Composable
   fun MyComposable(
       name: String,
       modifier: Modifier = Modifier
   ) { ... }
   ```
3. **Container:** Always wrap content in a container (`Column`, `Row`, `Box`) — do not emit content at the top level:

   ```kotlin
   // Wrong — behavior depends on parent composable
   @Composable
   fun MyComposable() {
       Text("Hello")
       Text("Foo")
   }

   // Correct
   @Composable
   fun MyComposable() {
       Column {
           Text("Hello")
           Text("Foo")
       }
   }

   // Also acceptable — scoped to parent
   @Composable
   fun ColumnScope.MyComposable() {
       Text("Hello")
       Text("Foo")
   }
   ```

4. **Immutable params only:** No `MutableList`, `MutableState`, etc. as parameters
5. **remember:** Always `remember {}` all `mutableStateOf` / `derivedStateOf`
6. **State delegates:** Use `by` for state: `var foo by rememberSaveable { mutableStateOf(1) }`
7. **State hoisting:** State up, events down via lambdas. Mutate state outside the composable scope (e.g., in `onClick {}` lambdas)
8. **No ViewModel inside composables:** Pass as parameter, never acquire with `viewModel()`

## Theming and Styling

- `composeView {}` already wraps content in `WooThemeWithBackground` — do not double-wrap
- For previews, wrap in `WooThemeWithBackground { ... }`
- Colors: `MaterialTheme.colorScheme.*` (Material 3)
- Typography: `MaterialTheme.typography.*`
- Theme files: `ui/compose/theme/` (Theme.kt, WooColors.kt, Typography.kt, Shapes.kt)
- Note: The project nests Material 2 inside Material 3 for backward compatibility

Use `WooThemeWithBackground` (not `WooTheme`) when the composable root does not support `contentColor`, to properly handle light/dark modes.

## Existing Components

Before creating new components, check `ui/compose/component/` for reusable ones:

| Component | File | Purpose |
|-----------|------|---------|
| `Toolbar` | `Toolbar.kt` | Top app bar with navigation, action buttons |
| `WCColoredButton` | `Buttons.kt` | Primary filled button |
| `WCOutlinedButton` | `Buttons.kt` | Secondary outlined button |
| `WCTextButton` | `Buttons.kt` | Tertiary text button |
| `WCRemoveButton` | `Buttons.kt` | Destructive outlined button (error color) |
| `WCPullToRefreshBox` | `WCPullToRefreshBox.kt` | Pull-to-refresh container |
| `WCModalBottomSheet` | `WCModalBottomSheet.kt` | Bottom sheet |
| `AlertDialog` | `AlertDialog.kt` | Alert dialog |
| `DialogState.Render()` | `DialogState.kt` | Data-driven dialog from ViewModel |
| `WCOutlinedSpinner` | `WCOutlinedSpinner.kt` | Dropdown spinner |
| `WCPrimaryTabRow` | `WCPrimaryTabRow.kt` | Tab row |
| `SearchLayoutWithParams` | `SearchLayoutWithParams.kt` | Search with filter params |
| `OverflowMenu` | `OverflowMenu.kt` | Three-dot overflow menu |
| `DatePickerDialog` | `DatePickerDialog.kt` | Date picker |
| `TimePickerDialog` | `TimePickerDialog.kt` | Time picker |
| `ProgressIndicator` | `ProgressIndicator.kt` | Loading indicator |

Also available: `annotatedStringRes()` and `clickableAnnotatedStringRes()` in `TextExts.kt`.

## Previews

Use the project's custom preview annotations for consistency:

```kotlin
@LightDarkThemePreviews  // Light + Dark mode
@Composable
private fun MyFeatureScreenPreview() {
    WooThemeWithBackground {
        MyFeatureScreen(
            viewState = MyFeatureViewState(title = "Preview"),
            onBack = {},
        )
    }
}
```

Available annotations from `ui/compose/preview/PreviewAnnotations.kt`:
- `@LightDarkThemePreviews` — light and dark mode (preferred for new code)
- `@OrientationPreviews` — landscape and portrait
- `@FontScalePreviews` — normal and large font
- `@LayoutDirectionPreviews` — LTR and RTL

## File Structure

- `ui/compose/component/` — shared components reused across features
- `ui/compose/theme/` — themes, colors, shapes, typography
- `ui/compose/animations/` — shared reusable animations
- `ui/compose/preview/` — preview annotations
- `ui/compose/modifier/` and `ui/compose/modifiers/` — shared modifier extensions
- `ui/<feature>/` — feature screens, ViewModels
- `ui/<feature>/compose/` — feature-specific composable sub-components

## Managing State

- Apply [state hoisting](https://developer.android.com/jetpack/compose/state#state-hoisting) whenever possible — move state out of `@Composable` functions to make them stateless
- Delegate data manipulation to the ViewModel or parent composable
- Use `by` property delegates: `var foo by rememberSaveable { mutableStateOf(1) }`
- Mutate state outside the composable scope (e.g., in `onClick {}` lambdas)
- Pass immutable values to composable functions
- Composable functions should be side-effect free. When needed, use [side-effect APIs](https://developer.android.com/jetpack/compose/side-effects)

## Navigation

Navigation uses XML nav graphs with `NavController`, unchanged from the pre-Compose approach. Compose screens are hosted inside Fragments (see [Fragment Hosting](#fragment-hosting)).

## Accessibility

- Set `contentDescription` on icons and images — describe the meaning, not appearance
- Use `Modifier.semantics(mergeDescendants = true)` to group content for TalkBack navigation
- Mark headings with `Modifier.semantics { heading() }`
- Add `stateDescription` for stateful items (selected/unselected):
  ```kotlin
  Modifier.semantics(mergeDescendants = true) {
      stateDescription = if (item.selected) selectedDescription else unselectedDescription
  }
  ```
- For composables from the Compose foundation/material library, semantics are generated automatically
- When adding custom low-level composables, provide semantics manually

## UI Tests in Compose

Compose provides testing APIs similar to Espresso. Add `ComposeTestRule` to interact with UI elements:

```kotlin
class MyComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()
}
```

Testing interactions:
- **Finders:** Select elements (nodes in the Semantics tree)
- **Assertions:** Verify elements exist or have certain attributes
- **Actions:** Inject simulated user events (clicks, gestures)

### Synchronization

Use `waitUntil` for async operations — **never** use `Thread.sleep()`:

```kotlin
composeTestRule.waitUntil {
    composeTestRule
        .onAllNodesWithText("Welcome")
        .fetchSemanticsNodes().size == 1
}
```
