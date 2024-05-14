package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.network.shippingmethods.ShippingMethodsRestClient
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload

@OptIn(ExperimentalCoroutinesApi::class)
class GetShippingMethodByIdTest : BaseUnitTest() {
    private val siteModel = SiteModel()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn siteModel
    }
    private val resourceProvider: ResourceProvider = mock()
    private val shippingMethodsRestClient: ShippingMethodsRestClient = mock()

    private val shippingMethodsRepository = ShippingMethodsRepository(
        selectedSite = selectedSite,
        dispatchers = coroutinesTestRule.testDispatchers,
        resourceProvider = resourceProvider,
        shippingMethodsRestClient = shippingMethodsRestClient
    )

    val sut = GetShippingMethodById(shippingMethodsRepository)

    @Test
    fun `when the method is in the result, then return is the expected`() = testBlocking {
        val methodId = "id1"
        val fetchResult = ShippingMethodsRestClient.ShippingMethodDto(
            id = methodId,
            title = "title1",
        )

        whenever(shippingMethodsRestClient.fetchShippingMethodsById(siteModel, methodId)).doReturn(
            WooPayload(fetchResult)
        )
        whenever(resourceProvider.getString(any())).doReturn("Other")

        val result = sut.invoke(methodId)
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(methodId)
    }

    @Test
    fun `when the method id is other, then return is the expected`() = testBlocking {
        whenever(resourceProvider.getString(any())).doReturn("Other")

        val result = sut.invoke(ShippingMethodsRepository.OTHER_ID)

        // If is other doesn't need to fetch the values from the API
        verify(shippingMethodsRestClient, never()).fetchShippingMethodsById(
            siteModel,
            ShippingMethodsRepository.OTHER_ID
        )
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(ShippingMethodsRepository.OTHER_ID)
    }

    @Test
    fun `when fetching shipping methods fail, then return other`() = testBlocking {
        val methodId = "id8"
        val fetchResult = WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)
        whenever(shippingMethodsRestClient.fetchShippingMethodsById(siteModel, methodId))
            .doReturn(WooPayload(fetchResult))
        whenever(resourceProvider.getString(any())).doReturn("Other")

        val result = sut.invoke(methodId)
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(ShippingMethodsRepository.OTHER_ID)
    }
}
