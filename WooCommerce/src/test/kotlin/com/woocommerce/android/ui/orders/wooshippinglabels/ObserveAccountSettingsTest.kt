package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingAccountSettingsDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AccountSettingsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodOptions
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
class ObserveAccountSettingsTest : BaseUnitTest() {
    private val dataStore: WooShippingAccountSettingsDataStore = mock()
    private val fetchAccountSettings: FetchAccountSettings = mock()
    private val defaultAccountSettings = AccountSettingsModel(
        storeOptions = StoreOptionsModel(
            weightUnit = "kg",
            currencySymbol = "$",
            dimensionUnit = "cm",
            originCountry = "US"
        ),
        paymentMethodOptions = PaymentMethodOptions(
            selectedPaymentId = null,
            paymentMethods = emptyList(),
            addPaymentMethodUrl = "https://example.com/add-payment-method"
        )
    )

    val sut = ObserveAccountSettings(
        configurationDataStore = dataStore,
        fetchAccountSettings = fetchAccountSettings
    )

    @Test
    fun `when there is cached data and fetch account settings fails then return cached data`() =
        testBlocking {
            whenever(dataStore.observeAccountSettings()).doReturn(flowOf(defaultAccountSettings))
            whenever(fetchAccountSettings.invoke()).doReturn(Result.failure(Exception("Random error")))
            val result = sut.invoke().toList()

            assertTrue(result.size == 1)
            assertTrue(result.first() == defaultAccountSettings)
        }

    @Test
    fun `when starting then refresh data once`() = testBlocking {
        whenever(dataStore.observeAccountSettings()).doReturn(
            flowOf(
                defaultAccountSettings,
                defaultAccountSettings.copy(
                    storeOptions = defaultAccountSettings.storeOptions.copy(currencySymbol = "€")
                )
            )
        )
        whenever(fetchAccountSettings.invoke()).doReturn(Result.failure(Exception("Random error")))

        sut.invoke().toList()

        verify(fetchAccountSettings).invoke()
    }
}
