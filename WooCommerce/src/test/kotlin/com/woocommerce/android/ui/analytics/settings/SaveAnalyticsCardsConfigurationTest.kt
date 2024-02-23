package com.woocommerce.android.ui.analytics.settings

import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.ui.analytics.hub.settings.SaveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsSettingsDataStore
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class SaveAnalyticsCardsConfigurationTest : BaseUnitTest() {
    private val analyticsSettingsDataStore: AnalyticsSettingsDataStore = mock()
    private lateinit var sut: SaveAnalyticsCardsConfiguration

    @Before
    fun setup() {
        sut = SaveAnalyticsCardsConfiguration(analyticsSettingsDataStore)
    }

    @Test
    fun `when the configuration is saved, not visible cards are sent to the back of the list`() = testBlocking {
        val configuration = listOf(
            AnalyticCardConfiguration(1, "Revenue", false),
            AnalyticCardConfiguration(2, "Orders", true),
            AnalyticCardConfiguration(2, "Visitors", false),
            AnalyticCardConfiguration(3, "Products", true)
        )
        val expected = listOf(

            AnalyticCardConfiguration(2, "Orders", true),
            AnalyticCardConfiguration(3, "Products", true),
            AnalyticCardConfiguration(1, "Revenue", false),
            AnalyticCardConfiguration(2, "Visitors", false)
        )
        sut.invoke(configuration)
        verify(analyticsSettingsDataStore).saveAnalyticsCardsConfiguration(expected)
    }
}
