package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingEligibilityDataStore
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingLabelRepositoryTest : BaseUnitTest() {
    private val restClient: WooShippingLabelRestClient = mock()
    private val eligibilityDataStore: WooShippingEligibilityDataStore = mock()
    private val repository = WooShippingLabelRepository(
        selectedSite = mock(),
        restClient = restClient,
        mapper = mock(),
        eligibilityDataStore = eligibilityDataStore,
        wooShippingDao = mock(),
        accountSettingsDataStore = mock(),
        addressDataStore = mock()
    )
    private val site = SiteModel()

    @Test
    fun `when order is eligible, then save and return eligible result`() = testBlocking {
        // GIVEN
        val eligibility = EligibilityResponse(isEligible = true)
        whenever(restClient.fetchShippingEligibility(site, ORDER_ID)).thenReturn(WooPayload(eligibility))

        // WHEN
        val result = repository.fetchShippingEligibility(site, ORDER_ID)

        // THEN
        verify(eligibilityDataStore).saveEligibility(ORDER_ID, true)
        assertThat(result.isError).isFalse()
        assertThat(result.model).isEqualTo(eligibility)
    }

    @Test
    fun `when order is ineligible, then save and return ineligible result`() = testBlocking {
        // GIVEN
        val eligibility = EligibilityResponse(isEligible = false)
        whenever(restClient.fetchShippingEligibility(site, ORDER_ID)).thenReturn(WooPayload(eligibility))

        // WHEN
        val result = repository.fetchShippingEligibility(site, ORDER_ID)

        // THEN
        verify(eligibilityDataStore).saveEligibility(ORDER_ID, false)
        assertThat(result.isError).isFalse()
        assertThat(result.model).isEqualTo(eligibility)
    }

    @Test
    fun `when response has null eligibility, then save null and return successful result`() = testBlocking {
        // GIVEN
        val eligibility = EligibilityResponse(isEligible = null)
        whenever(restClient.fetchShippingEligibility(site, ORDER_ID)).thenReturn(WooPayload(eligibility))

        // WHEN
        val result = repository.fetchShippingEligibility(site, ORDER_ID)

        // THEN
        verify(eligibilityDataStore).saveEligibility(ORDER_ID, null)
        assertThat(result.isError).isFalse()
        assertThat(result.model).isEqualTo(eligibility)
    }

    @Test
    fun `when eligibility request fails, then leave stored eligibility untouched and return error`() = testBlocking {
        // GIVEN
        val error = WooError(type = GENERIC_ERROR, original = UNKNOWN)
        whenever(restClient.fetchShippingEligibility(site, ORDER_ID)).thenReturn(WooPayload(error))

        // WHEN
        val result = repository.fetchShippingEligibility(site, ORDER_ID)

        // THEN
        verifyNoInteractions(eligibilityDataStore)
        assertThat(result.isError).isTrue()
        assertThat(result.error).isSameAs(error)
    }

    private companion object {
        const val ORDER_ID = 1L
    }
}
