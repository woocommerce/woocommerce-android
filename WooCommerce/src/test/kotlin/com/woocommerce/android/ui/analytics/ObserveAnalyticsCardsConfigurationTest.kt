package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.analytics.hub.GetAnalyticPluginsCardActive
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.settings.SaveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsSettingsDataStore
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsSettingsResourcesRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ObserveAnalyticsCardsConfigurationTest : BaseUnitTest() {
    private val settingsDataStore: AnalyticsSettingsDataStore = mock()
    private val resourcesRepository: AnalyticsSettingsResourcesRepository = mock()
    private val getAnalyticPluginsCardActive: GetAnalyticPluginsCardActive = mock()
    private val saveAnalyticsCardsConfiguration: SaveAnalyticsCardsConfiguration = mock()

    private lateinit var sut: ObserveAnalyticsCardsConfiguration

    private val defaultConfiguration = AnalyticsCards.entries.map { card ->
        AnalyticCardConfiguration(card, card.name, true)
    }

    private val savedConfiguration =
        defaultConfiguration.mapIndexed { i, item -> if (i > 3) item.copy(isVisible = false) else item }

    private fun initializeViewModel() {
        sut = ObserveAnalyticsCardsConfiguration(
            settingsDataStore,
            resourcesRepository,
            getAnalyticPluginsCardActive,
            saveAnalyticsCardsConfiguration
        )
    }

    @Test
    fun `when there is NO saved configuration, the default configuration is retrieved`() = testBlocking {
        val defaultPluginCardsActive = setOf(AnalyticsCards.Bundles, AnalyticsCards.Session)
        whenever(resourcesRepository.getDefaultAnalyticsCardsConfiguration()).thenReturn(defaultConfiguration)
        whenever(settingsDataStore.observeCardsConfiguration()).thenReturn(flowOf(null))
        whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)

        initializeViewModel()
        val result = sut.invoke().first()
        assertThat(result).isEqualTo(defaultConfiguration)
    }

    @Test
    fun `when there is a configuration saved, the saved configuration is retrieved`() = testBlocking {
        val defaultPluginCardsActive = setOf(AnalyticsCards.Bundles, AnalyticsCards.Session)

        whenever(settingsDataStore.observeCardsConfiguration()).thenReturn(flowOf(savedConfiguration))
        whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(defaultPluginCardsActive)

        initializeViewModel()
        val result = sut.invoke().first()
        assertThat(result).isEqualTo(savedConfiguration)
        verify(resourcesRepository, never()).getDefaultAnalyticsCardsConfiguration()
    }

    @Test
    fun `when the configuration contains plugin card as visible but the plugin is not active, then hide the card`() =
        testBlocking {
            val pluginCardsActive = emptySet<AnalyticsCards>()
            whenever(settingsDataStore.observeCardsConfiguration()).thenReturn(flowOf(defaultConfiguration))
            whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(pluginCardsActive)

            initializeViewModel()
            val result = sut.invoke().first()
            assertThat(result).contains(AnalyticCardConfiguration(AnalyticsCards.Bundles, "Bundles", false))
        }

    @Test
    fun `when the configuration contains plugin card as visible and the plugin is active, then show the card`() =
        testBlocking {
            val pluginCardsActive = setOf(AnalyticsCards.Bundles, AnalyticsCards.Session)
            whenever(settingsDataStore.observeCardsConfiguration()).thenReturn(flowOf(defaultConfiguration))
            whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(pluginCardsActive)

            initializeViewModel()
            val result = sut.invoke().first()
            assertThat(result).contains(AnalyticCardConfiguration(AnalyticsCards.Bundles, "Bundles", true))
        }

    @Test
    fun `when the saved configuration has an outdated cards number, then merge the outdated configuration with the default one`() =
        testBlocking {
            val pluginCardsActive = setOf(AnalyticsCards.Bundles, AnalyticsCards.Session)
            val configuration = defaultConfiguration.map { it.copy(isVisible = false) }.dropLast(2)
            whenever(resourcesRepository.getDefaultAnalyticsCardsConfiguration()).thenReturn(defaultConfiguration)
            whenever(settingsDataStore.observeCardsConfiguration()).thenReturn(flowOf(configuration))
            whenever(getAnalyticPluginsCardActive.invoke()).thenReturn(pluginCardsActive)

            initializeViewModel()
            val result = sut.invoke().first()
            assertThat(result.size).isEqualTo(AnalyticsCards.entries.size)
            val visibleCards = result.count { it.isVisible }
            // There is only 2 cards visible, the cards that were added from the merge with the default configuration
            assertThat(visibleCards).isEqualTo(2)
            verify(saveAnalyticsCardsConfiguration).invoke(any())
        }
}
