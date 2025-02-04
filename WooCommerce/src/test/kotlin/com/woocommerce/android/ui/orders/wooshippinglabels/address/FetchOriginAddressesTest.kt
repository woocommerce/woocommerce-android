package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
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
class FetchOriginAddressesTest : BaseUnitTest() {
    private val shippingRepository: WooShippingLabelRepository = mock()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() } doReturn SiteModel().apply {
            url = "https://example.com"
        }
    }

    private val defaultOriginAddresses = OriginShippingAddress(
        id = "1",
        firstName = "John",
        lastName = "Doe",
        company = "",
        address1 = "123 Main St",
        address2 = "",
        city = "Anytown",
        state = "CA",
        postcode = "12345",
        country = "US",
        email = "william.henry.harrison@example-pet-store.com",
        phone = "555-555-5555",
        isDefault = true,
        isVerified = true
    )

    val sut = FetchOriginAddresses(
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
    fun `when fetch origin addresses fails then return failure`() = testBlocking {
        whenever(shippingRepository.fetchOriginAddresses(any())).doReturn(
            WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
        )

        val result = sut.invoke()
        assert(result.isFailure)
    }

    @Test
    fun `when fetch origin addresses is empty then return failure`() = testBlocking {
        whenever(shippingRepository.fetchOriginAddresses(any())).doReturn(
            WooResult(emptyList())
        )

        val result = sut.invoke()
        assert(result.isFailure)
    }

    @Test
    fun `when fetch origin addresses succeed then return success`() = testBlocking {
        whenever(shippingRepository.fetchOriginAddresses(any())).doReturn(
            WooResult(listOf(defaultOriginAddresses))
        )

        val result = sut.invoke()
        assert(result.isSuccess)
    }
}
