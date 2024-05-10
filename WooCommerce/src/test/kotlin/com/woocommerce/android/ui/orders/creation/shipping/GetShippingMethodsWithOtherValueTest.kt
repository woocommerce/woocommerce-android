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
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload

@OptIn(ExperimentalCoroutinesApi::class)
class GetShippingMethodsWithOtherValueTest : BaseUnitTest() {

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

    val sut = GetShippingMethodsWithOtherValue(shippingMethodsRepository)

    @Test
    fun `when  get shipping succeed then a success response is returned including other`() = testBlocking {
        val fetchResult = List(3) { i ->
            ShippingMethodsRestClient.ShippingMethodDto(
                id = "id$i",
                title = "title$i",
            )
        }
        whenever(shippingMethodsRestClient.fetchShippingMethods(siteModel)).doReturn(WooPayload(fetchResult))
        whenever(resourceProvider.getString(any())).doReturn("Other")

        val result = sut.invoke()
        assertThat(result).isNotNull
        assertThat(result.isSuccess).isTrue()
        assertThat(result.isFailure).isFalse()
        assertThat(result.getOrNull()!!.size).isEqualTo(4) // List + Other
        val other = result.getOrNull()!!.firstOrNull { it.id == ShippingMethodsRepository.OTHER_ID }
        assertThat(other).isNotNull
    }

    @Test
    fun `when  get shipping fails then an error response is returned`() = testBlocking {
        val fetchResult = WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)
        whenever(shippingMethodsRestClient.fetchShippingMethods(siteModel)).doReturn(WooPayload(fetchResult))

        val result = sut.invoke()
        assertThat(result).isNotNull
        assertThat(result.isSuccess).isFalse()
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `when get shipping succeed but is null then an error response is returned`() = testBlocking {
        val fetchResult = null
        whenever(shippingMethodsRestClient.fetchShippingMethods(siteModel)).doReturn(WooPayload(fetchResult))

        val result = sut.invoke()
        assertThat(result).isNotNull
        assertThat(result.isSuccess).isFalse()
        assertThat(result.isFailure).isTrue()
    }
}
