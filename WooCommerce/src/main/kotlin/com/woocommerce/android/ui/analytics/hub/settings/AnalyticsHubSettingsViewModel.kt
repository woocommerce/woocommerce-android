package com.woocommerce.android.ui.analytics.hub.settings

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.model.PluginUrls
import com.woocommerce.android.ui.analytics.hub.GetAnalyticPluginsCardActive
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.settings.AnalyticsHubSettingsViewModel.Companion.MARKETPLACE
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
    private val getAnalyticPluginsCardActive: GetAnalyticPluginsCardActive,
    private val tracker: AnalyticsTrackerWrapper,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    companion object {
        const val LOADING_DELAY_MS = 500L
        const val MARKETPLACE = "https://woocommerce.com/products/"
    }

    val viewStateData: LiveDataDelegate<AnalyticsHubSettingsViewState> =
        LiveDataDelegate(savedState, AnalyticsHubSettingsViewState.Loading)

    private var viewState by viewStateData
    private lateinit var currentConfiguration: List<AnalyticCardConfigurationUI>
    private lateinit var draftConfiguration: List<AnalyticCardConfigurationUI>
    private lateinit var activePluginCards: Set<AnalyticsCards>

    private fun hasChanges() = currentConfiguration != draftConfiguration

    init {
        launch {
            activePluginCards = getAnalyticPluginsCardActive()
            observeAnalyticsCardsConfiguration().first().let { configuration ->
                currentConfiguration = configuration
                    .map { it.toConfigurationUI(activePluginCards) }
                    .sortedByDescending { it is AnalyticCardConfigurationUI.SelectableCardConfigurationUI }
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
            trackSettingsSaved(configuration)
            delay(LOADING_DELAY_MS)
            triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    private fun trackSettingsSaved(configuration: List<AnalyticCardConfiguration>) {
        val enabledCards = mutableListOf<String>()
        val disabledCards = mutableListOf<String>()
        configuration.forEach { cardConfiguration ->
            val cardName = cardConfiguration.card.name.lowercase()
            if (cardConfiguration.isVisible) {
                enabledCards.add(cardName)
            } else {
                disabledCards.add(cardName)
            }
        }
        tracker.track(
            AnalyticsEvent.ANALYTICS_HUB_SETTINGS_SAVED,
            mapOf(
                AnalyticsTracker.KEY_ENABLED_CARDS to enabledCards.joinToString(","),
                AnalyticsTracker.KEY_DISABLED_CARDS to disabledCards.joinToString(","),
            )
        )
    }

    fun onSelectionChange(selectedItem: AnalyticCardConfigurationUI, isSelected: Boolean) {
        updateSelection(selectedItem.card, isSelected)
        checkVisibleCards()
        viewState = AnalyticsHubSettingsViewState.CardsConfiguration(
            cardsConfiguration = draftConfiguration,
            showDiscardDialog = false,
            isSaveButtonEnabled = hasChanges()
        )
    }

    private fun updateSelection(card: AnalyticsCards, isSelected: Boolean) {
        draftConfiguration = draftConfiguration.map { cardConfiguration ->
            when {
                cardConfiguration.card == card &&
                    cardConfiguration is AnalyticCardConfigurationUI.SelectableCardConfigurationUI -> {
                    cardConfiguration.copy(isVisible = isSelected)
                }

                else -> {
                    cardConfiguration
                }
            }
        }
    }

    fun onExploreUrl(url: String) {
        triggerEvent(MultiLiveEvent.Event.LaunchUrlInChromeTab(url))
    }

    private fun checkVisibleCards() {
        val visibleCards = draftConfiguration.count { card -> card.isVisible }
        draftConfiguration =
            draftConfiguration.map { card ->
                when {
                    card is AnalyticCardConfigurationUI.SelectableCardConfigurationUI &&
                        visibleCards == 1 &&
                        card.isVisible -> card.copy(isEnabled = false)
                    card is AnalyticCardConfigurationUI.SelectableCardConfigurationUI -> card.copy(isEnabled = true)
                    else -> card
                }
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
sealed class AnalyticCardConfigurationUI(
    open val card: AnalyticsCards,
    open val title: String,
    open val isVisible: Boolean,
) : Parcelable {

    data class SelectableCardConfigurationUI(
        override val card: AnalyticsCards,
        override val title: String,
        override val isVisible: Boolean = true,
        val isEnabled: Boolean = true
    ) : AnalyticCardConfigurationUI(card, title, isVisible)

    data class ExploreCardConfigurationUI(
        override val card: AnalyticsCards,
        override val title: String,
        val url: String,
    ) : AnalyticCardConfigurationUI(card, title, false)
}

fun AnalyticCardConfiguration.toConfigurationUI(activePluginCards: Set<AnalyticsCards>): AnalyticCardConfigurationUI {
    return if (this.card.isPlugin.not() || this.card in activePluginCards) {
        AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
            card = this.card,
            title = this.title,
            isVisible = this.isVisible,
            isEnabled = true
        )
    } else {
        val url = when (this.card) {
            AnalyticsCards.Bundles -> PluginUrls.BUNDLES_URL
            AnalyticsCards.GiftCards -> PluginUrls.GIFT_CARDS_URL
            AnalyticsCards.Session -> PluginUrls.JETPACK_URL
            else -> MARKETPLACE
        }
        AnalyticCardConfigurationUI.ExploreCardConfigurationUI(
            card = this.card,
            title = this.title,
            url = url
        )
    }
}

fun AnalyticCardConfigurationUI.toConfigurationModel(): AnalyticCardConfiguration {
    return AnalyticCardConfiguration(
        card = this.card,
        title = this.title,
        isVisible = this.isVisible,
    )
}
