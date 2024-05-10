package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.network.shippingmethods.ShippingMethodsRestClient
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload

@OptIn(ExperimentalCoroutinesApi::class)
class ShippingMethodsRepositoryTest : BaseUnitTest() {

    private val selectedSite: SelectedSite = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val shippingMethodsRestClient: ShippingMethodsRestClient = mock()

    private val sut = ShippingMethodsRepository(
        selectedSite = selectedSite,
        dispatchers = coroutinesTestRule.testDispatchers,
        resourceProvider = resourceProvider,
        shippingMethodsRestClient = shippingMethodsRestClient
    )

    @Test
    fun `when shipping requests succeed then a success response is returned`() = testBlocking {
        val siteModel = SiteModel()
        val fetchResult = List(3) { i ->
            ShippingMethodsRestClient.ShippingMethodDto(
                id = "id$i",
                title = "title$i",
            )
        }
        whenever(shippingMethodsRestClient.fetchShippingMethods(siteModel)).doReturn(WooPayload(fetchResult))

        val result = sut.fetchShippingMethods(siteModel)
        assertThat(result.isError).isFalse()
        assertThat(result.model).isNotNull
        assertThat(result.model?.size).isEqualTo(fetchResult.size)
    }

    @Test
    fun `when shipping requests succeed but model is null then an error response is returned`() = testBlocking {
        val siteModel = SiteModel()
        val fetchResult = null
        whenever(shippingMethodsRestClient.fetchShippingMethods(siteModel)).doReturn(WooPayload(fetchResult))

        val result = sut.fetchShippingMethods(siteModel)
        assertThat(result.isError).isTrue()
        assertThat(result.model).isNull()
    }

    @Test
    fun `when shipping requests fails then an error response is returned`() = testBlocking {
        val siteModel = SiteModel()
        val fetchResult = WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)
        whenever(shippingMethodsRestClient.fetchShippingMethods(siteModel)).doReturn(WooPayload(fetchResult))

        val result = sut.fetchShippingMethods(siteModel)
        assertThat(result.isError).isTrue()
        assertThat(result.model).isNull()
    }
}
