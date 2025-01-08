package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigurationDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveStoreOptionsTest : BaseUnitTest() {
    private val dataStore: WooShippingConfigurationDataStore = mock()
    private val fetchAccountSettings: FetchAccountSettings = mock()
    private val defaultStoreOptions = StoreOptionsModel(
        weightUnit = "kg",
        currencySymbol = "$",
        dimensionUnit = "cm",
        originCountry = "US"
    )

    val sut = ObserveStoreOptions(
        configurationDataStore = dataStore,
        fetchAccountSettings = fetchAccountSettings
    )

    @Test
    fun `when there is NO cached data and fetch account settings fails then return null`() = testBlocking {
        whenever(dataStore.observeStoreOptions()).doReturn(flowOf(null))
        whenever(fetchAccountSettings.invoke()).doReturn(Result.failure(Exception("Random error")))
        val result = sut.invoke().toList()

        assertTrue(result.size == 1)
        assertTrue(result.first() == null)
    }

    @Test
    fun `when there is cached data and fetch account settings fails then return null`() = testBlocking {
        whenever(dataStore.observeStoreOptions()).doReturn(flowOf(defaultStoreOptions))
        whenever(fetchAccountSettings.invoke()).doReturn(Result.failure(Exception("Random error")))
        val result = sut.invoke().toList()

        assertTrue(result.size == 2)
        assertTrue(result.first() == defaultStoreOptions)
        assertTrue(result.last() == null)
    }

    @Test
    fun `refresh data only once`() = testBlocking {
        whenever(dataStore.observeStoreOptions()).doReturn(
            flowOf(defaultStoreOptions, defaultStoreOptions.copy(currencySymbol = "€"))
        )
        whenever(fetchAccountSettings.invoke()).doReturn(Result.failure(Exception("Random error")))

        sut.invoke().toList()

        verify(fetchAccountSettings).invoke()
    }
}
