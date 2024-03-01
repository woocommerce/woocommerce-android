package com.woocommerce.android.ui.analytics.hub.settings

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
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
    private lateinit var currentConfiguration: List<AnalyticCardConfigurationUI>
    private lateinit var draftConfiguration: List<AnalyticCardConfigurationUI>

    private fun hasChanges() = currentConfiguration != draftConfiguration

    init {
        launch {
            delay(LOADING_DELAY_MS)
            observeAnalyticsCardsConfiguration().first().let { configuration ->
                currentConfiguration = configuration.map { it.toConfigurationUI() }
                draftConfiguration = currentConfiguration
                checkVisibleCards()
                viewState = AnalyticsHubSettingsViewState.CardsConfiguration(
                    cardsConfiguration = draftConfiguration,
                    showDiscardDialog = false,
                    isSaveButtonEnabled = false
                )
            }
        }
    }

    fun onBackPressed() {
        when {
            viewState is AnalyticsHubSettingsViewState.CardsConfiguration && hasChanges() -> {
                viewState = AnalyticsHubSettingsViewState.CardsConfiguration(
                    cardsConfiguration = draftConfiguration,
                    showDiscardDialog = true,
                    isSaveButtonEnabled = hasChanges()
                )
            }

            else -> triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    fun onDismissDiscardChanges() {
        viewState = AnalyticsHubSettingsViewState.CardsConfiguration(
            cardsConfiguration = draftConfiguration,
            showDiscardDialog = false,
            isSaveButtonEnabled = hasChanges()
        )
    }

    fun onDiscardChanges() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onSaveChanges() {
        launch {
            viewState = AnalyticsHubSettingsViewState.Loading
            val configuration = draftConfiguration.map { it.toConfigurationModel() }
            saveAnalyticsCardsConfiguration(configuration)
            delay(LOADING_DELAY_MS)
            triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    fun onSelectionChange(card: AnalyticsCards, isSelected: Boolean) {
        updateSelection(card, isSelected)
        checkVisibleCards()
        viewState = AnalyticsHubSettingsViewState.CardsConfiguration(
            cardsConfiguration = draftConfiguration,
            showDiscardDialog = false,
            isSaveButtonEnabled = hasChanges()
        )
    }

    private fun updateSelection(card: AnalyticsCards, isSelected: Boolean) {
        draftConfiguration = draftConfiguration.map { cardConfiguration ->
            if (cardConfiguration.card == card) cardConfiguration.copy(isVisible = isSelected)
            else cardConfiguration
        }
    }

    private fun checkVisibleCards() {
        val visibleCards = draftConfiguration.count { card -> card.isVisible }
        draftConfiguration = draftConfiguration.map { card ->
            if (visibleCards == 1 && card.isVisible) card.copy(isEnabled = false)
            else card.copy(isEnabled = true)
        }
    }

    fun onOrderChange(fromIndex: Int, toIndex: Int) {
        draftConfiguration = draftConfiguration.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        viewState = AnalyticsHubSettingsViewState.CardsConfiguration(
            cardsConfiguration = draftConfiguration,
            showDiscardDialog = false,
            isSaveButtonEnabled = hasChanges()
        )
    }
}

@Parcelize
sealed class AnalyticsHubSettingsViewState : Parcelable {
    data class CardsConfiguration(
        val cardsConfiguration: List<AnalyticCardConfigurationUI>,
        val isSaveButtonEnabled: Boolean,
        val showDiscardDialog: Boolean = false
    ) : AnalyticsHubSettingsViewState()

    data object Loading : AnalyticsHubSettingsViewState()
}

@Parcelize
data class AnalyticCardConfigurationUI(
    val card: AnalyticsCards,
    val title: String,
    val isVisible: Boolean = true,
    val isEnabled: Boolean = true
) : Parcelable

fun AnalyticCardConfiguration.toConfigurationUI(): AnalyticCardConfigurationUI {
    return AnalyticCardConfigurationUI(
        card = this.card,
        title = this.title,
        isVisible = this.isVisible,
        isEnabled = true
    )
}

fun AnalyticCardConfigurationUI.toConfigurationModel(): AnalyticCardConfiguration {
    return AnalyticCardConfiguration(
        card = this.card,
        title = this.title,
        isVisible = this.isVisible,
    )
}
