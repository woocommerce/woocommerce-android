package com.woocommerce.android.ui.analytics.hub.settings

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.SaveAnalyticsCardsConfiguration
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class AnalyticsHubSettingsViewModel @Inject constructor(
    private val observeAnalyticsCardsConfiguration: ObserveAnalyticsCardsConfiguration,
    private val saveAnalyticsCardsConfiguration: SaveAnalyticsCardsConfiguration,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    companion object {
        const val LOADING_DELAY_MS = 500L
    }

    val viewStateData: LiveDataDelegate<AnalyticsHubSettingsViewState> =
        LiveDataDelegate(savedState, AnalyticsHubSettingsViewState.Loading)

    private var viewState by viewStateData
    private lateinit var currentConfiguration: List<AnalyticCardConfiguration>
    private lateinit var draftConfiguration: List<AnalyticCardConfiguration>

    private fun hasChanges() = currentConfiguration != draftConfiguration

    init {
        launch {
            delay(LOADING_DELAY_MS)
            observeAnalyticsCardsConfiguration().first().let {
                currentConfiguration = it
                draftConfiguration = currentConfiguration
                viewState = AnalyticsHubSettingsViewState.CardsConfiguration(
                    cardsConfiguration = draftConfiguration,
                    showDismissDialog = false,
                    isSaveButtonEnabled = hasChanges()
                )
            }
        }
    }

    fun onBackPressed() {
        when {
            viewState is AnalyticsHubSettingsViewState.CardsConfiguration && hasChanges() -> {
                viewState = AnalyticsHubSettingsViewState.CardsConfiguration(
                    cardsConfiguration = draftConfiguration,
                    showDismissDialog = true,
                    isSaveButtonEnabled = hasChanges()
                )
            }
            else -> triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    fun onDismissDiscardChanges() {
        viewState = AnalyticsHubSettingsViewState.CardsConfiguration(
            cardsConfiguration = draftConfiguration,
            showDismissDialog = false,
            isSaveButtonEnabled = hasChanges()
        )
    }

    fun onDiscardChanges() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onSaveChanges() {
        launch {
            viewState = AnalyticsHubSettingsViewState.Loading
            saveAnalyticsCardsConfiguration(draftConfiguration)
            delay(LOADING_DELAY_MS)
            triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    fun onSelectionChange(id: Int, isSelected: Boolean) {
        draftConfiguration = draftConfiguration.map { card ->
            if (card.id == id) card.copy(isVisible = isSelected)
            else card
        }
        viewState = AnalyticsHubSettingsViewState.CardsConfiguration(
            cardsConfiguration = draftConfiguration,
            showDismissDialog = false,
            isSaveButtonEnabled = hasChanges()
        )
    }
}

@Parcelize
sealed class AnalyticsHubSettingsViewState : Parcelable {
    data class CardsConfiguration(
        val cardsConfiguration: List<AnalyticCardConfiguration>,
        val isSaveButtonEnabled: Boolean,
        val showDismissDialog: Boolean = false
    ) : AnalyticsHubSettingsViewState()

    data object Loading : AnalyticsHubSettingsViewState()
}

@Parcelize
data class AnalyticCardConfiguration(
    val id: Int,
    val title: String,
    val isVisible: Boolean
) : Parcelable
