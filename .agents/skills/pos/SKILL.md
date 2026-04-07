---
name: pos
description: POS (Point of Sale) architecture and patterns. Use when writing, editing, exploring, debugging, or reviewing WooPos-prefixed classes or files under ui/woopos/. POS uses a different architecture than the main app — plain ViewModel (not ScopedViewModel), pure Compose (no Fragments), Compose Navigation (no nav graphs), parent-child SharedFlow event bus, WooPosAnalyticsTracker, and WooPosCoroutineTestRule. Loading this skill prevents applying main-app patterns that would be wrong for POS.
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

## POS Analytics

Use `WooPosAnalyticsTracker` (not `AnalyticsTrackerWrapper`). It's a suspend function that switches to `Dispatchers.IO`:

```kotlin
viewModelScope.launch {
    analyticsTracker.track(WooPosAnalyticsEvent.Event.MyActionTapped)
}
```

Events are defined as data objects/classes inside `WooPosAnalyticsEvent.Event`:
```kotlin
// In WooPosAnalyticsEvent.kt
data object MyActionTapped : Event() {
    override val name: String = "my_action_tapped"
}
```

For errors use `WooPosAnalyticsEvent.Error`:
```kotlin
data class MyActionFailed(
    override val errorContext: KClass<out Any>,
    override val errorType: String?,
    override val errorDescription: String?,
) : Error() {
    override val name: String = "my_action_failed"
}
```

## POS Tests

Do NOT extend `BaseUnitTest`. Use `WooPosCoroutineTestRule` and `runTest`:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class WooPosMyFeatureViewModelTest {
    @Rule @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()

    @Test
    fun `when action clicked, then event is tracked`() = runTest {
        whenever(parentToChildrenEventReceiver.events).thenReturn(flowOf())
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUIEvent(WooPosMyFeatureUIEvent.ActionClicked)

        verify(analyticsTracker).track(argThat { this is WooPosAnalyticsEvent.Event.ActionTapped })
    }

    private fun createViewModel() = WooPosMyFeatureViewModel(
        parentToChildrenEventReceiver = parentToChildrenEventReceiver,
        childrenToParentEventSender = childrenToParentEventSender,
        analyticsTracker = analyticsTracker,
    )
}
```

Key testing differences:
- Mock `WooPosParentToChildrenEventReceiver.events` — return `flowOf()` for default, `MutableSharedFlow` when testing event handling
- Mock `WooPosChildrenToParentEventSender` — verify `sendToParent()` calls
- Use `advanceUntilIdle()` after creating ViewModel (to let init coroutines run)
- Use `argThat { this is EventType }` for analytics verification (POS events are data objects)
- Use `runTest` (not `testBlocking`)

## File Locations

- Source: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/`
- Tests: `WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/`
- Analytics: `.../ui/woopos/util/analytics/`
- Event bus: `.../ui/woopos/home/WooPosHomeChildToParentCommunication.kt` and `WooPosHomeParentToChildCommunication.kt`
- Navigation: `.../ui/woopos/home/WooPosHomeNavigation.kt`
- Activity: `.../ui/woopos/root/WooPosActivity.kt`
