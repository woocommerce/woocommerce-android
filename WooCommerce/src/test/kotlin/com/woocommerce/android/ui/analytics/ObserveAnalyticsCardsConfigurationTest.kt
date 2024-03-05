package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsSettingsDataStore
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsSettingsResourcesRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ObserveAnalyticsCardsConfigurationTest : BaseUnitTest() {
    private val settingsDataStore: AnalyticsSettingsDataStore = mock()
    private val resourcesRepository: AnalyticsSettingsResourcesRepository = mock()

    private lateinit var sut: ObserveAnalyticsCardsConfiguration

    private fun initializeViewModel() {
        sut = ObserveAnalyticsCardsConfiguration(settingsDataStore, resourcesRepository)
    }

    @Test
    fun `when there is NO saved configuration, the default configuration is retrieved`() = testBlocking {
        val defaultConfiguration = listOf(
            AnalyticCardConfiguration(AnalyticsCards.Revenue, "Revenue", true),
            AnalyticCardConfiguration(AnalyticsCards.Orders, "Orders", true),
            AnalyticCardConfiguration(AnalyticsCards.Session, "Visitors", true),
            AnalyticCardConfiguration(AnalyticsCards.Products, "Products", true)
        )
        whenever(resourcesRepository.getDefaultAnalyticsCardsConfiguration()).thenReturn(defaultConfiguration)
        whenever(settingsDataStore.observeCardsConfiguration()).thenReturn(flowOf(null))

        initializeViewModel()
        val result = sut.invoke().first()
        assertThat(result).isEqualTo(defaultConfiguration)
    }

    @Test
    fun `when there is a configuration saved, the saved configuration is retrieved`() = testBlocking {
        val defaultConfiguration = listOf(
            AnalyticCardConfiguration(AnalyticsCards.Revenue, "Revenue", true),
            AnalyticCardConfiguration(AnalyticsCards.Orders, "Orders", true),
            AnalyticCardConfiguration(AnalyticsCards.Session, "Visitors", true),
            AnalyticCardConfiguration(AnalyticsCards.Products, "Products", true)
        )

        val savedConfiguration = listOf(
            AnalyticCardConfiguration(AnalyticsCards.Session, "Visitors", true),
            AnalyticCardConfiguration(AnalyticsCards.Orders, "Orders", true),
            AnalyticCardConfiguration(AnalyticsCards.Products, "Products", true),
            AnalyticCardConfiguration(AnalyticsCards.Revenue, "Revenue", false),
        )

        whenever(resourcesRepository.getDefaultAnalyticsCardsConfiguration()).thenReturn(defaultConfiguration)
        whenever(settingsDataStore.observeCardsConfiguration()).thenReturn(flowOf(savedConfiguration))

        initializeViewModel()
        val result = sut.invoke().first()
        assertThat(result).isEqualTo(savedConfiguration)
    }
}
