package com.woocommerce.android.ui.orders.wooshippinglabels.address.origin

import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@OptIn(ExperimentalCoroutinesApi::class)
class NormalizeAddressTest : BaseUnitTest() {

    private val repository: WooShippingLabelRepository = mock()
    private val site: SelectedSite = mock()

    private val sut = NormalizeAddress(repository, site)
    private val defaultAddress = Address.EMPTY
    private val defaultNormalizeAddressResponse = AddressNormalizationModel(
        address = Address.EMPTY.copy(postcode = "1001"),
        normalizedAddress = Address.EMPTY.copy(postcode = "1001-900"),
        isTrivial = false
    )

    @Test
    fun `when selected site is null then return failure`() = testBlocking {
        val result = sut.invoke(defaultAddress)

        assert(result.isFailure)
    }

    @Test
    fun `when normalize address fails then return failure`() = testBlocking {
        whenever(site.getOrNull()).thenReturn(SiteModel())
        whenever(repository.normalizeAddress(any(), any())).thenReturn(WooResult(WooError(GENERIC_ERROR, UNKNOWN)))

        val result = sut.invoke(defaultAddress)

        assert(result.isFailure)
    }

    @Test
    fun `when normalize address succeed then return expected data`() = testBlocking {
        whenever(site.getOrNull()).thenReturn(SiteModel())
        whenever(repository.normalizeAddress(any(), any())).thenReturn(WooResult(defaultNormalizeAddressResponse))

        val result = sut.invoke(defaultAddress)

        assert(result.isSuccess)
        assertThat(result.getOrNull()).isEqualTo(defaultNormalizeAddressResponse)
    }
}
