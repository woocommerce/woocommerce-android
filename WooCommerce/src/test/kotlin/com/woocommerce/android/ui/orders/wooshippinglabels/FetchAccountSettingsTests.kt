package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FetchAccountSettingsTests : BaseUnitTest() {
    private val shippingRepository: WooShippingLabelRepository = mock()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() } doReturn SiteModel().apply {
            url = "https://example.com"
        }
    }

    private val defaultStoreOptions = StoreOptionsModel(
        weightUnit = "kg",
        currencySymbol = "$",
        dimensionUnit = "cm",
        originCountry = "US"
    )

    val sut = FetchAccountSettings(
        shippingRepository = shippingRepository,
        selectedSite = selectedSite
    )

    @Test
    fun `when selected site is null then return failure`() = testBlocking {
        whenever(selectedSite.getOrNull()).doReturn(null)
        val result = sut.invoke()
        assert(result.isFailure)
    }

    @Test
    fun `when fetch account settings fails then return failure`() = testBlocking {
        whenever(shippingRepository.fetchAccountSettings(any())).doReturn(
            WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
        )

        val result = sut.invoke()
        assert(result.isFailure)
    }

    @Test
    fun `when fetch account settings is empty then return failure`() = testBlocking {
        whenever(shippingRepository.fetchAccountSettings(any())).doReturn(
            WooResult(StoreOptionsModel.EMPTY)
        )

        val result = sut.invoke()
        assert(result.isFailure)
    }

    @Test
    fun `when fetch account settings succeed then return success`() = testBlocking {
        whenever(shippingRepository.fetchAccountSettings(any())).doReturn(
            WooResult(defaultStoreOptions)
        )

        val result = sut.invoke()
        assert(result.isSuccess)
    }
}
