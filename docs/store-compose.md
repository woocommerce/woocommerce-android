# Store App — Jetpack Compose Guidelines

> POS has its own Compose patterns — see [POS Architecture](pos-architecture.md).

We follow the official [Compose API guidelines for App development](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#api-guidelines-for-jetpack-compose) and Google's Compose best practices. This doc covers only **project-specific** conventions and patterns.

## Content

1. [Fragment Hosting](#fragment-hosting)
2. [Screen Composable Pattern](#screen-composable-pattern)
3. [Project-Specific Conventions](#project-specific-conventions)
4. [Existing Components](#existing-components)
5. [Previews](#previews)
6. [File Structure](#file-structure)

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

## Project-Specific Conventions

- Use `UPPER_SNAKE_CASE` for constants (overrides the Compose guideline suggesting `PascalCase`)
- The project nests Material 2 inside Material 3 for backward compatibility
- `composeView {}` already wraps content in `WooThemeWithBackground` — do not double-wrap
- For previews, wrap in `WooThemeWithBackground { ... }`
- Theme files: `ui/compose/theme/` (Theme.kt, WooColors.kt, Typography.kt, Shapes.kt)
- Navigation uses XML nav graphs with `NavController` — Compose screens are hosted inside Fragments

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
