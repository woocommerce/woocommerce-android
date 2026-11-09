package com.woocommerce.android.ui.login.storesuggestion

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginStoreSuggestionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val getInstallReferrerStoreSuggestion: GetInstallReferrerStoreSuggestion,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    private val suggestion = savedState.getStateFlow<StoreSuggestion?>(KEY_SUGGESTION, null)
    private val isDismissRequested = MutableStateFlow(false)

    private var hasStarted = false

    val viewState: StateFlow<ViewState?> = combine(suggestion, isDismissRequested) { suggestion, dismissRequested ->
        suggestion?.let {
            ViewState(
                host = it.host,
                registrableDomain = it.registrableDomain,
                isDismissRequested = dismissRequested,
            )
        }
    }.toStateFlow(null)

    fun start(isAllowed: Boolean) {
        if (hasStarted || !isAllowed) return
        hasStarted = true
        launch {
            getInstallReferrerStoreSuggestion()?.let {
                savedState[KEY_SUGGESTION] = it
                analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_STORE_SUGGESTION_SHOWN)
            }
        }
    }

    fun onLogInToStoreClicked() {
        val suggestion = suggestion.value ?: return
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_STORE_SUGGESTION_ACCEPTED)
        savedState[KEY_SUGGESTION] = null
        triggerEvent(LogInToStore(siteUrl = suggestion.siteUrl, host = suggestion.host))
    }

    fun onUseDifferentStoreClicked() {
        if (suggestion.value == null) return
        isDismissRequested.value = true
    }

    fun onDismissed() {
        if (suggestion.value == null) return
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_STORE_SUGGESTION_DISMISSED)
        savedState[KEY_SUGGESTION] = null
        isDismissRequested.value = false
    }

    data class ViewState(
        val host: String,
        val registrableDomain: String?,
        val isDismissRequested: Boolean,
    )

    data class LogInToStore(val siteUrl: String, val host: String) : Event()

    private companion object {
        const val KEY_SUGGESTION = "store_suggestion"
    }
}
