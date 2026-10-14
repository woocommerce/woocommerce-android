package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiers
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiersRequirement.NOT_REQUIRED
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiersRequirement.REQUIRED
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.CustomsRequirementsResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@OptIn(ExperimentalCoroutinesApi::class)
class CheckEuProductIdentifiersRequirementTest : BaseUnitTest() {
    private val site = SiteModel()
    private val repository: WooShippingLabelRepository = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val sut = CheckEuProductIdentifiersRequirement(repository, selectedSite)

    @Test
    fun `when EU identifiers are required, then return requirement and product identifiers`() = testBlocking {
        val productIdentifiers = mapOf(PRODUCT_ID to EuProductIdentifiers("SKU", "MPN", "GTIN"))
        whenever(repository.fetchCustomsRequirements(site, ORDER_ID, ORIGIN_ID))
            .thenReturn(WooResult(CustomsRequirementsResponse(euProductIdsRequired = true)))
        whenever(repository.fetchEuProductIdentifiers(site, ORDER_ID)).thenReturn(WooResult(productIdentifiers))

        val result = sut(ORDER_ID, ORIGIN_ID)

        assertThat(result.requirement).isEqualTo(REQUIRED)
        assertThat(result.productIdentifiers).isEqualTo(productIdentifiers)
    }

    @Test
    fun `when EU identifiers are not required, then skip loading product identifiers`() = testBlocking {
        whenever(repository.fetchCustomsRequirements(site, ORDER_ID, ORIGIN_ID))
            .thenReturn(WooResult(CustomsRequirementsResponse(euProductIdsRequired = false)))

        val result = sut(ORDER_ID, ORIGIN_ID)

        assertThat(result.requirement).isEqualTo(NOT_REQUIRED)
        assertThat(result.productIdentifiers).isEmpty()
        verify(repository, never()).fetchEuProductIdentifiers(site, ORDER_ID)
    }

    @Test
    fun `when customs requirement check fails, then treat it as not required`() = testBlocking {
        whenever(repository.fetchCustomsRequirements(site, ORDER_ID, ORIGIN_ID)).thenReturn(
            WooResult(WooError(type = GENERIC_ERROR, original = UNKNOWN, message = "Request failed"))
        )

        val result = sut(ORDER_ID, ORIGIN_ID)

        assertThat(result.requirement).isEqualTo(NOT_REQUIRED)
        assertThat(result.productIdentifiers).isEmpty()
        verify(repository, never()).fetchEuProductIdentifiers(site, ORDER_ID)
    }

    @Test
    fun `when required identifiers cannot be loaded, then keep requirement and return an empty map`() = testBlocking {
        whenever(repository.fetchCustomsRequirements(site, ORDER_ID, ORIGIN_ID))
            .thenReturn(WooResult(CustomsRequirementsResponse(euProductIdsRequired = true)))
        whenever(repository.fetchEuProductIdentifiers(site, ORDER_ID)).thenReturn(
            WooResult(WooError(type = GENERIC_ERROR, original = UNKNOWN, message = "Config failed"))
        )

        val result = sut(ORDER_ID, ORIGIN_ID)

        assertThat(result.requirement).isEqualTo(REQUIRED)
        assertThat(result.productIdentifiers).isEmpty()
    }

    private companion object {
        const val ORDER_ID = 123L
        const val ORIGIN_ID = "origin-id"
        const val PRODUCT_ID = 456L
    }
}
