---
name: pos
description: POS (Point of Sale) architecture and patterns. Use when writing, editing, exploring, debugging, or reviewing WooPos-prefixed classes or files under ui/woopos/. POS uses a different architecture than the main app — plain ViewModel (not ScopedViewModel), pure Compose (no Fragments), Compose Navigation (no nav graphs), parent-child SharedFlow event bus. Loading this skill prevents applying main-app patterns that would be wrong for POS. For analytics use `pos-analytics`, for tests use `pos-tests`.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# POS Architecture & Patterns

POS has its own architecture. Do NOT apply main-app patterns (ScopedViewModel, Fragments, MultiLiveEvent, nav graphs) to POS code.

## POS ViewModel

Extend plain `ViewModel()` — NOT `ScopedViewModel`:

```kotlin
@HiltViewModel
class WooPosMyFeatureViewModel @Inject constructor(
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(WooPosMyFeatureState())
    val state: StateFlow<WooPosMyFeatureState> = _state.asStateFlow()

    init {
        listenEventsFromParent()
    }

    private fun listenEventsFromParent() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.SomeEvent -> handleEvent(event)
                    else -> Unit
                }
            }
        }
    }

    fun onUIEvent(event: WooPosMyFeatureUIEvent) {
        when (event) {
            is WooPosMyFeatureUIEvent.ActionClicked -> {
                viewModelScope.launch {
                    analyticsTracker.track(WooPosAnalyticsEvent.Event.ActionTapped)
                    // do work
                }
            }
        }
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(event)
        }
    }
}
```

### Key differences from main app

| Aspect | POS | Main App |
|--------|-----|----------|
| Base class | `ViewModel()` | `ScopedViewModel(savedStateHandle)` |
| Coroutines | `viewModelScope.launch {}` | `launch {}` (from CoroutineScope) |
| State | `StateFlow<T>` | `StateFlow<T>` or `LiveData<T>` |
| Events | Parent-child SharedFlow bus | `triggerEvent()` / `MultiLiveEvent` |
| Analytics | `WooPosAnalyticsTracker` | `AnalyticsTrackerWrapper` |
| Navigation | Compose Navigation | Fragment nav graphs |

## Parent-Child Event Bus

POS screens communicate via a SharedFlow-based event bus scoped to `ActivityRetainedComponent`.

**Children → Parent** (sending events up):
```kotlin
// Inject sender
private val childrenToParentEventSender: WooPosChildrenToParentEventSender,

// Send
viewModelScope.launch {
    childrenToParentEventSender.sendToParent(ChildToParentEvent.CheckoutClicked)
}
```

**Parent → Children** (receiving events from above):
```kotlin
// Inject receiver
private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,

// Listen in init
viewModelScope.launch {
    parentToChildrenEventReceiver.events.collect { event ->
        when (event) {
            is ParentToChildrenEvent.OrderSuccessfullyPaid -> handleSuccess(event)
            else -> Unit
        }
    }
}
```

Event definitions:
- `ChildToParentEvent` — sealed class in `WooPosHomeChildToParentCommunication.kt`
- `ParentToChildrenEvent` — sealed class in `WooPosHomeParentToChildCommunication.kt`

## UI Events Pattern

POS ViewModels accept UI events via a single `onUIEvent()` method with a sealed class:

```kotlin
sealed class WooPosMyFeatureUIEvent {
    data class ItemClicked(val id: Long) : WooPosMyFeatureUIEvent()
    data object BackClicked : WooPosMyFeatureUIEvent()
}
```

## State Classes

All POS state classes MUST be `@Parcelize` + `: Parcelable` for process-death recovery:

```kotlin
@Parcelize
data class WooPosMyFeatureState(
    val isLoading: Boolean = false,
    val items: List<WooPosMyItem> = emptyList(),
) : Parcelable
```

Use sealed classes for distinct screen states:
```kotlin
sealed class WooPosMyViewState : Parcelable {
    @Parcelize data class Loading(...) : WooPosMyViewState()
    @Parcelize data class Content(...) : WooPosMyViewState()
    @Parcelize data class Error(...) : WooPosMyViewState()
}
```

## POS Compose UI

POS is 100% Compose — no Fragments, no `composeView {}`, no XML layouts.

Entry point is `WooPosActivity` which calls `setContent { WooPosRootScreen() }`.

Screens are pure `@Composable` functions that collect state from ViewModels:

```kotlin
@Composable
fun WooPosMyFeatureScreen(
    modifier: Modifier = Modifier,
    viewModel: WooPosMyFeatureViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    WooPosMyFeatureScreen(
        state = state,
        onUIEvent = viewModel::onUIEvent,
        modifier = modifier,
    )
}

@Composable
fun WooPosMyFeatureScreen(
    state: WooPosMyFeatureState,
    onUIEvent: (WooPosMyFeatureUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // stateless UI
}
```

Key differences from main app Compose:
- Use `hiltViewModel()` to acquire VMs (no Fragment-level `by viewModels()`)
- Use `collectAsState()` for StateFlow (not `observeAsState()`)
- Pass events via `onUIEvent` callback (not individual lambdas)
- Navigation uses Compose Navigation (`NavHost`, `composable()`) — see `WooPosHomeNavigation.kt`
- Landscape-only, tablet-optimized

## Design System

POS has its own design system — do NOT use main app's `WooTheme`. All POS UI must be wrapped in `WooPosTheme`.

### Theme

`WooPosTheme` — Material 3 theme with light/dark support. Use `MaterialTheme.colorScheme.*` for standard colors and `WooPosTheme.colors.*` for POS custom colors (success, alert, disabled, info, error variants). See `WooPosTheme.kt` for available custom colors.

### Spacing

Use `WooPosSpacing` enum — values scale adaptively based on screen size. Never use raw `dp` values for padding/margins:

```kotlin
Modifier.padding(WooPosSpacing.Medium.value)
```

See `WooPosSizes.kt` for available sizes and adaptive scaling logic.

### Corner Radius and Elevation

Use `WooPosCornerRadius` and `WooPosElevation` enums (fixed, not adaptive):

```kotlin
RoundedCornerShape(WooPosCornerRadius.Medium.value)
Modifier.shadow(elevation = WooPosElevation.Medium.value)
```

### Typography

Use `WooPosTypography` sealed class with the `WooPosText` component:

```kotlin
WooPosText(
    text = "Title",
    style = WooPosTypography.Heading,
    color = MaterialTheme.colorScheme.onSurface,
)
```

See `WooPosTypography.kt` for available styles.

### Design System Source

All definitions in `.../ui/woopos/common/composeui/designsystem/` — read these files for current values:
- `WooPosTheme.kt` — theme, color schemes, custom colors
- `WooPosSizes.kt` — spacing, corner radius, elevation enums
- `WooPosTypography.kt` — text styles
- `WooPosIcons.kt` — theme-aware icons (very large file — avoid reading unless you need to find a specific icon)

## File Locations

- Source: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/`
- Tests: `WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/`
- Analytics: `.../ui/woopos/util/analytics/`
- Event bus: `.../ui/woopos/home/WooPosHomeChildToParentCommunication.kt` and `WooPosHomeParentToChildCommunication.kt`
- Navigation: `.../ui/woopos/home/WooPosHomeNavigation.kt`
- Activity: `.../ui/woopos/root/WooPosActivity.kt`
