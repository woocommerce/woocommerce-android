package com.woocommerce.android.ui.analytics.settings

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.model.PluginUrls
import com.woocommerce.android.ui.analytics.hub.GetAnalyticPluginsCardActive
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.settings.AnalyticCardConfigurationUI
import com.woocommerce.android.ui.analytics.hub.settings.AnalyticsHubSettingsViewModel
import com.woocommerce.android.ui.analytics.hub.settings.AnalyticsHubSettingsViewState
import com.woocommerce.android.ui.analytics.hub.settings.AnalyticsHubSettingsViewState.CardsConfiguration
import com.woocommerce.android.ui.analytics.hub.settings.SaveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.settings.toConfigurationUI
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class AnalyticsHubSettingsViewModelTest : BaseUnitTest() {
    private val observeAnalyticsCardsConfiguration: ObserveAnalyticsCardsConfiguration = mock()
    private val saveAnalyticsCardsConfiguration: SaveAnalyticsCardsConfiguration = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val savedState: SavedStateHandle = SavedStateHandle()
    private val getAnalyticPluginsCardActive: GetAnalyticPluginsCardActive = mock()

    private lateinit var sut: AnalyticsHubSettingsViewModel

    private val defaultConfiguration = listOf(
        AnalyticCardConfiguration(AnalyticsCards.Revenue, "Revenue", true),
        AnalyticCardConfiguration(AnalyticsCards.Orders, "Orders", true),
        AnalyticCardConfiguration(AnalyticsCards.Bundles, "Bundles", true),
        AnalyticCardConfiguration(AnalyticsCards.Session, "Visitors", false)
    )

    private val defaultPluginCardsActive = setOf(AnalyticsCards.Bundles, AnalyticsCards.Session)

    private val defaultConfigurationUI = defaultConfiguration.map { it.toConfigurationUI(defaultPluginCardsActive) }

    fun setup() {
        sut = AnalyticsHubSettingsViewModel(
            observeAnalyticsCardsConfiguration = observeAnalyticsCardsConfiguration,
            saveAnalyticsCardsConfiguration = saveAnalyticsCardsConfiguration,
            tracker = tracker,
            savedState = savedState,
            getAnalyticPluginsCardActive = getAnalyticPluginsCardActive
        )
    }

    @Test
    fun `when back is pressed without changes then exit the screen`() = testBlocking {
        whenever(observeAnalyticsCardsConfiguration.invoke()).thenReturn(flowOf(defaultConfiguration))
        whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)
        setup()

        var event: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent -> event = latestEvent }

        var viewState: AnalyticsHubSettingsViewState? = null
        sut.viewStateData.observeForever { _, new -> viewState = new }

        sut.onBackPressed()

        // The exit event is triggered
        assertEquals(MultiLiveEvent.Event.Exit, event)
        // The discard dialog is not displayed
        assertThat(viewState).isInstanceOf(CardsConfiguration::class.java)
        assertThat((viewState as CardsConfiguration).showDiscardDialog).isEqualTo(false)
    }

    @Test
    fun `when back is pressed with changes then display the discard dialog`() = testBlocking {
        whenever(observeAnalyticsCardsConfiguration.invoke()).thenReturn(flowOf(defaultConfiguration))
        whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)
        setup()

        var event: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent -> event = latestEvent }

        var viewState: AnalyticsHubSettingsViewState? = null
        sut.viewStateData.observeForever { _, new -> viewState = new }

        advanceTimeBy(501)

        sut.onSelectionChange(defaultConfigurationUI.last(), true)
        sut.onBackPressed()

        // The exit event is NOT triggered
        assertNotEquals(MultiLiveEvent.Event.Exit, event)
        // The discard dialog is displayed
        assertThat(viewState).isInstanceOf(CardsConfiguration::class.java)
        assertThat((viewState as CardsConfiguration).showDiscardDialog).isEqualTo(true)
    }

    @Test
    fun `when the screen is displayed save button is disabled`() = testBlocking {
        whenever(observeAnalyticsCardsConfiguration.invoke()).thenReturn(flowOf(defaultConfiguration))
        whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)
        setup()

        var viewState: AnalyticsHubSettingsViewState? = null
        sut.viewStateData.observeForever { _, new -> viewState = new }

        advanceTimeBy(501)

        // The save button is disabled when the configuration doesn't have any change
        assertThat(viewState).isInstanceOf(CardsConfiguration::class.java)
        assertThat((viewState as CardsConfiguration).isSaveButtonEnabled).isEqualTo(false)
    }

    @Test
    fun `when the screen is displayed and some change are made, the save button is enabled`() = testBlocking {
        whenever(observeAnalyticsCardsConfiguration.invoke()).thenReturn(flowOf(defaultConfiguration))
        whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)
        setup()

        var viewState: AnalyticsHubSettingsViewState? = null
        sut.viewStateData.observeForever { _, new -> viewState = new }

        advanceTimeBy(501)

        sut.onSelectionChange(defaultConfigurationUI.last(), true)

        // The save button is disabled when the configuration doesn't have any change
        assertThat(viewState).isInstanceOf(CardsConfiguration::class.java)
        assertThat((viewState as CardsConfiguration).isSaveButtonEnabled).isEqualTo(true)
    }

    @Test
    fun `when the configuration is changed and the save button is pressed, then the updated configuration is saved`() =
        testBlocking {
            whenever(observeAnalyticsCardsConfiguration.invoke()).thenReturn(flowOf(defaultConfiguration))
            whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)
            setup()

            val itemsToChange = defaultConfigurationUI.take(3)
            val itemsToChangeCards = itemsToChange.map { it.card }.toSet()
            val expectedConfiguration = defaultConfiguration.map {
                if (it.card in itemsToChangeCards) {
                    it.copy(isVisible = false)
                } else {
                    it
                }
            }

            advanceTimeBy(501)

            itemsToChange.forEach { card ->
                sut.onSelectionChange(card, false)
            }

            sut.onSaveChanges()

            verify(saveAnalyticsCardsConfiguration).invoke(expectedConfiguration)
        }

    @Test
    fun `when the received configuration only have one selected card, then the selected card is disabled`() =
        testBlocking {
            val configuration = listOf(
                AnalyticCardConfiguration(AnalyticsCards.Revenue, "Revenue", true),
                AnalyticCardConfiguration(AnalyticsCards.Orders, "Orders", false),
                AnalyticCardConfiguration(AnalyticsCards.Session, "Stats", false)
            )
            val expected = listOf(
                AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
                    AnalyticsCards.Revenue,
                    "Revenue",
                    true,
                    isEnabled = false
                ),
                AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
                    AnalyticsCards.Orders,
                    "Orders",
                    false,
                    isEnabled = true
                ),
                AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
                    AnalyticsCards.Session,
                    "Stats",
                    false,
                    isEnabled = true
                )
            )
            whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)
            whenever(observeAnalyticsCardsConfiguration.invoke()).thenReturn(flowOf(configuration))
            setup()

            var viewState: AnalyticsHubSettingsViewState? = null
            sut.viewStateData.observeForever { _, new -> viewState = new }

            advanceTimeBy(501)

            // The save button is disabled when the configuration doesn't have any change
            assertThat(viewState).isInstanceOf(CardsConfiguration::class.java)
            assertThat((viewState as CardsConfiguration).cardsConfiguration).isEqualTo(expected)
        }

    @Test
    fun `when configuration only have 1 selected card and other card is selected, then all cards are enabled`() =
        testBlocking {
            val configuration = listOf(
                AnalyticCardConfiguration(AnalyticsCards.Revenue, "Revenue", true),
                AnalyticCardConfiguration(AnalyticsCards.Orders, "Orders", false),
                AnalyticCardConfiguration(AnalyticsCards.Session, "Stats", false)
            )
            val expected = listOf(
                AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
                    AnalyticsCards.Revenue,
                    "Revenue",
                    true,
                    isEnabled = true
                ),
                AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
                    AnalyticsCards.Orders,
                    "Orders",
                    true,
                    isEnabled = true
                ),
                AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
                    AnalyticsCards.Session,
                    "Stats",
                    false,
                    isEnabled = true
                )
            )
            whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)
            whenever(observeAnalyticsCardsConfiguration.invoke()).thenReturn(flowOf(configuration))
            setup()

            var viewState: AnalyticsHubSettingsViewState? = null
            sut.viewStateData.observeForever { _, new -> viewState = new }

            advanceTimeBy(501)

            sut.onSelectionChange(
                AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
                    AnalyticsCards.Orders,
                    "Orders",
                    false,
                    isEnabled = true
                ),
                true
            )

            // The save button is disabled when the configuration doesn't have any change
            assertThat(viewState).isInstanceOf(CardsConfiguration::class.java)
            assertThat((viewState as CardsConfiguration).cardsConfiguration).isEqualTo(expected)
        }

    @Test
    fun `when configuration have 2 selected card and one of those cards is deselected, then the selected card is disabled`() =
        testBlocking {
            val configuration = listOf(
                AnalyticCardConfiguration(AnalyticsCards.Revenue, "Revenue", true),
                AnalyticCardConfiguration(AnalyticsCards.Orders, "Orders", true),
                AnalyticCardConfiguration(AnalyticsCards.Session, "Stats", false)
            )
            val expected = listOf(
                AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
                    AnalyticsCards.Revenue,
                    "Revenue",
                    true,
                    isEnabled = false
                ),
                AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
                    AnalyticsCards.Orders,
                    "Orders",
                    false,
                    isEnabled = true
                ),
                AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
                    AnalyticsCards.Session,
                    "Stats",
                    false,
                    isEnabled = true
                )
            )
            whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)
            whenever(observeAnalyticsCardsConfiguration.invoke()).thenReturn(flowOf(configuration))
            setup()

            var viewState: AnalyticsHubSettingsViewState? = null
            sut.viewStateData.observeForever { _, new -> viewState = new }

            advanceTimeBy(501)

            sut.onSelectionChange(
                AnalyticCardConfigurationUI.SelectableCardConfigurationUI(
                    AnalyticsCards.Orders,
                    "Orders",
                    true,
                    isEnabled = true
                ),
                false
            )

            // The save button is disabled when the configuration doesn't have any change
            assertThat(viewState).isInstanceOf(CardsConfiguration::class.java)
            assertThat((viewState as CardsConfiguration).cardsConfiguration).isEqualTo(expected)
        }

    @Test
    fun `when an order change event is triggered, then the card's order is the expected`() = testBlocking {
        val configuration = listOf(
            AnalyticCardConfiguration(AnalyticsCards.Revenue, "Revenue", true),
            AnalyticCardConfiguration(AnalyticsCards.Orders, "Orders", true),
            AnalyticCardConfiguration(AnalyticsCards.Session, "Stats", false)
        )
        val expected = listOf(
            AnalyticCardConfigurationUI.SelectableCardConfigurationUI(AnalyticsCards.Orders, "Orders", true),
            AnalyticCardConfigurationUI.SelectableCardConfigurationUI(AnalyticsCards.Session, "Stats", false),
            AnalyticCardConfigurationUI.SelectableCardConfigurationUI(AnalyticsCards.Revenue, "Revenue", true),
        )
        whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)
        whenever(observeAnalyticsCardsConfiguration.invoke()).thenReturn(flowOf(configuration))
        setup()

        var viewState: AnalyticsHubSettingsViewState? = null
        sut.viewStateData.observeForever { _, new -> viewState = new }

        advanceTimeBy(501)

        sut.onOrderChange(0, 2)

        // The save button is disabled when the configuration doesn't have any change
        assertThat(viewState).isInstanceOf(CardsConfiguration::class.java)
        assertThat((viewState as CardsConfiguration).cardsConfiguration).isEqualTo(expected)
    }

    @Test
    fun `when the save button is pressed, then analytics save event is triggered with the expected params`() =
        testBlocking {
            val configuration = listOf(
                AnalyticCardConfiguration(AnalyticsCards.Revenue, "Revenue", true),
                AnalyticCardConfiguration(AnalyticsCards.Orders, "Orders", true),
                AnalyticCardConfiguration(AnalyticsCards.Session, "Stats", false)
            )

            val expectedEnableCards = configuration.filter { it.isVisible }.map { it.card.name.lowercase() }
            val expectedDisabledCards = configuration.filter { it.isVisible.not() }.map { it.card.name.lowercase() }

            whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)
            whenever(observeAnalyticsCardsConfiguration.invoke()).thenReturn(flowOf(configuration))
            setup()

            advanceTimeBy(501)

            sut.onSaveChanges()
            verify(tracker).track(
                AnalyticsEvent.ANALYTICS_HUB_SETTINGS_SAVED,
                mapOf(
                    AnalyticsTracker.KEY_ENABLED_CARDS to expectedEnableCards.joinToString(","),
                    AnalyticsTracker.KEY_DISABLED_CARDS to expectedDisabledCards.joinToString(","),
                )
            )
        }

    @Test
    fun `when a plugin is not active, then the plugin card is explore option`() = testBlocking {
        whenever(observeAnalyticsCardsConfiguration.invoke()).thenReturn(flowOf(defaultConfiguration))
        whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(emptySet())
        setup()

        var viewState: AnalyticsHubSettingsViewState? = null
        sut.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState).isInstanceOf(CardsConfiguration::class.java)
        assertThat((viewState as CardsConfiguration).cardsConfiguration).contains(
            AnalyticCardConfigurationUI.ExploreCardConfigurationUI(
                card = AnalyticsCards.Bundles,
                title = AnalyticsCards.Bundles.name,
                url = PluginUrls.BUNDLES_URL
            )
        )
    }
}
