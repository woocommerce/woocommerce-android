# Store App — ViewModel Patterns

> POS ViewModels use a different pattern — see [POS Architecture](pos-architecture.md).
- Everything else → **Store Management**

## Store App ViewModel

### Full Example

```kotlin
@HiltViewModel
class MyFeatureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MyRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val navArgs by savedStateHandle.navArgs<MyFeatureFragmentArgs>()

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        launch {
            _viewState.update { it.copy(isLoading = true) }
            repository.fetchData(navArgs.itemId)
                .onSuccess { result ->
                    _viewState.update { it.copy(isLoading = false, data = result) }
                }
                .onFailure {
                    _viewState.update { it.copy(isLoading = false) }
                    triggerEvent(ShowSnackbar(R.string.error_generic))
                }
        }
    }

    fun onBackClick() {
        triggerEvent(Exit)
    }

    data class ViewState(
        val isLoading: Boolean = false,
        val data: List<Item> = emptyList()
    )

    companion object {
        const val RESULT_KEY = "my_feature_result"
    }
}
```

### Key Rules

1. Extend `ScopedViewModel(savedStateHandle)`
2. Annotate with `@HiltViewModel` + `@Inject constructor`
3. `SavedStateHandle` MUST be the first constructor parameter
4. MUST NOT import Android framework classes (`Context`, `View`, etc.) — use `ResourceProvider` for strings
5. Use `StateFlow` for new state (keep existing `LiveData`, don't migrate)
6. Use `launch {}` for async work (inherits `viewModelScope` from `ScopedViewModel`)
7. Access data only through repositories, never Room/network directly
8. `companion object` MUST be placed at the bottom of the class

### Navigation Arguments

Retrieve fragment args via the `navArgs` delegate on `SavedStateHandle`:

```kotlin
import com.woocommerce.android.viewmodel.navArgs

private val navArgs by savedStateHandle.navArgs<MyFeatureFragmentArgs>()
```

Access in `init` or methods: `navArgs.itemId`, `navArgs.isEditing`, etc.

### State Patterns

#### Simple: MutableStateFlow

```kotlin
private val _viewState = MutableStateFlow(ViewState())
val viewState = _viewState.asStateFlow()

// Update immutably
_viewState.update { it.copy(isLoading = true) }
```

#### Composed: combine multiple sources

Combine multiple state sources into a single exposed state. Common when mixing savedState flows, repository flows, or multiple MutableStateFlows:

```kotlin
private val searchQuery = savedStateHandle.getStateFlow(viewModelScope, initialValue = "", key = "query")
private val isLoading = MutableStateFlow(false)

// For new code — expose as StateFlow
val viewState = combine(searchQuery, isLoading, repository.items) { query, loading, items ->
    ViewState(query = query, isLoading = loading, items = items.filter { it.matches(query) })
}.toStateFlow(initialValue = ViewState())

// Existing pattern — some ViewModels use .asLiveData() instead (don't migrate)
```

#### Process-death-safe: savedState flows

Use `savedState.getStateFlow()` for state that survives process death. Value must be `Parcelable`, `Serializable`, or primitive:

```kotlin
private val draft = savedStateHandle.getStateFlow(
    scope = viewModelScope,
    initialValue = navArgs.item ?: Item(),
    key = "draft"
)
```

For nullable types use `getNullableStateFlow()`:

```kotlin
private val errorMessage = savedStateHandle.getNullableStateFlow(
    scope = viewModelScope, initialValue = null, clazz = UiString::class.java, key = "error"
)
```

#### Flow to StateFlow conversion

Use the `toStateFlow()` helper from `ScopedViewModel` (uses `WhileSubscribed(5000)`):

```kotlin
val items = repository.observeItems().toStateFlow(initialValue = emptyList())
```

### View State Classes

Use `data class` for simple states:

```kotlin
data class ViewState(
    val isLoading: Boolean = false,
    val items: List<Item> = emptyList(),
    val errorMessage: UiString? = null
)
```

Use `sealed interface` for distinct screen states:

```kotlin
sealed interface ViewState {
    object Loading : ViewState
    object Error : ViewState
    data class Success(val items: List<Item>) : ViewState
}
```

Mark with `@Parcelize` + `: Parcelable` when persisted via `getStateFlow`:

```kotlin
@Parcelize
data class EditorState(
    val title: String = "",
    val showDiscardDialog: Boolean = false
) : Parcelable
```

### Events (One-shot UI Actions)

#### Built-in events

```kotlin
triggerEvent(ShowSnackbar(R.string.error_message))       // snackbar with string resource
triggerEvent(ShowUiStringSnackbar(UiString.UiStringRes(R.string.msg)))
triggerEvent(Exit)                                        // navigate back
triggerEvent(ExitWithResult(data = myResult, key = RESULT_KEY))
triggerEvent(LaunchUrlInChromeTab(url))
```

#### Custom events

Define inside the ViewModel class:

```kotlin
data class NavigateToDetail(val itemId: Long) : MultiLiveEvent.Event()

// Usage
triggerEvent(NavigateToDetail(item.id))
```

### Common Dependencies

| Dependency | Purpose |
|---|---|
| `AnalyticsTrackerWrapper` | Track events (never use `AnalyticsTracker` singleton directly) |
| `ResourceProvider` | Resolve string resources without importing `Context` |
| `SavedStateHandle` | Navigation args + process-death-safe state |
| Repositories | Data access (fetch, observe, update) |

## POS ViewModel

POS ViewModels use a different pattern — see [POS Architecture](pos-architecture.md).
