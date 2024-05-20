package com.woocommerce.android.ui.orders.creation.shipping

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
import org.wordpress.android.fluxc.model.WCShippingMethod
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCShippingMethodsStore

@OptIn(ExperimentalCoroutinesApi::class)
class GetShippingMethodByIdTest : BaseUnitTest() {
    private val siteModel = SiteModel()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn siteModel
    }
    private val resourceProvider: ResourceProvider = mock()
    private val shippingMethodsStore: WCShippingMethodsStore = mock()

    private val shippingMethodsRepository = ShippingMethodsRepository(
        selectedSite = selectedSite,
        dispatchers = coroutinesTestRule.testDispatchers,
        resourceProvider = resourceProvider,
        shippingMethodsStore = shippingMethodsStore
    )

    val sut = GetShippingMethodById(shippingMethodsRepository)

    @Test
    fun `when the method is in the result, then return is the expected`() = testBlocking {
        val methodId = "id1"
        val fetchResult = WCShippingMethod(
            id = methodId,
            title = "title1",
        )

        whenever(shippingMethodsStore.fetchShippingMethod(siteModel, methodId)).doReturn(WooResult(fetchResult))
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
        verify(shippingMethodsStore, never()).fetchShippingMethod(
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
        whenever(shippingMethodsStore.fetchShippingMethod(siteModel, methodId)).doReturn(WooResult(fetchResult))
        whenever(resourceProvider.getString(any())).doReturn("Other")

        val result = sut.invoke(methodId)
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(ShippingMethodsRepository.OTHER_ID)
    }
}
