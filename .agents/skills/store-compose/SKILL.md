---
name: store-compose
description: Main app Jetpack Compose UI patterns (WooTheme, Fragment hosting, composeView helper, preview annotations, WC components). Use when writing, editing, exploring, debugging, or reviewing Compose UI in the store management app. NOT for POS (WooPos*) code — use the `pos` skill instead.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# Store App Compose UI

## Fragment Hosting

Prefer the `composeView {}` extension (from `com.woocommerce.android.ui.compose.composeView`). It handles `DisposeOnViewTreeLifecycleDestroyed` and `WooThemeWithBackground` automatically:

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
- Do NOT create XML layouts -- use `composeView {}` directly

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

## Composable Function Rules

1. **Naming:** PascalCase noun names for `@Composable` functions returning Unit
2. **Modifier:** MUST accept `Modifier` as first optional parameter, named `modifier`
3. **Container:** Always wrap content in a container (`Column`, `Row`, `Box`)
4. **Immutable params only:** No `MutableList`, `MutableState`, etc.
5. **remember:** Always `remember {}` all `mutableStateOf` / `derivedStateOf`
6. **State delegates:** Use `by` for state: `var foo by rememberSaveable { mutableStateOf(1) }`
7. **State hoisting:** State up, events down via lambdas
8. **No ViewModel inside composables:** Pass as parameter, never acquire with `viewModel()`

## Theming

- `composeView {}` already wraps content in `WooThemeWithBackground` -- do not double-wrap
- For previews, wrap in `WooThemeWithBackground { ... }`
- Colors: `MaterialTheme.colorScheme.*` (Material 3)
- Typography: `MaterialTheme.typography.*`
- Theme files: `ui/compose/theme/` (Theme.kt, WooColors.kt, Typography.kt, Shapes.kt)
- Note: The project nests Material 2 inside Material 3 for backward compatibility

## Existing WC Components

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
- `@LightDarkThemePreviews` -- light and dark mode (preferred for new code)
- `@OrientationPreviews` -- landscape and portrait
- `@FontScalePreviews` -- normal and large font
- `@LayoutDirectionPreviews` -- LTR and RTL

## Constants

Use `UPPER_SNAKE_CASE` for constants (project convention overrides Compose guidelines).

## File Structure

- `ui/compose/component/` -- shared components reused across features
- `ui/compose/theme/` -- themes, colors, shapes, typography
- `ui/compose/animations/` -- shared reusable animations
- `ui/compose/preview/` -- preview annotations
- `ui/compose/modifier/` and `ui/compose/modifiers/` -- shared modifier extensions
- `ui/<feature>/` -- feature screens, ViewModels
- `ui/<feature>/compose/` -- feature-specific composable sub-components

## Accessibility Checklist

- Set `contentDescription` on icons and images (describe meaning, not appearance)
- Use `Modifier.semantics(mergeDescendants = true)` to group related content for TalkBack
- Mark headings with `Modifier.semantics { heading() }`
- Add `stateDescription` for stateful items (selected/unselected)
