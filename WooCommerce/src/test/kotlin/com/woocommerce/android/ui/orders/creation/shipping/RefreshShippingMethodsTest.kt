package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCShippingMethod
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCShippingMethodsStore

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshShippingMethodsTest : BaseUnitTest() {
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

    val sut = RefreshShippingMethods(shippingMethodsRepository)

    @Test
    fun `when the methods succeed, then return is the expected`() = testBlocking {
        val methodId = "id1"
        val methodTitle = "title1"
        val fetchResult = listOf(WCShippingMethod(methodId, methodTitle))
        val expected = listOf(ShippingMethod(methodId, methodTitle))

        whenever(shippingMethodsStore.fetchShippingMethods(siteModel)).doReturn(WooResult(fetchResult))

        val result = sut.invoke()
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isSuccess).isTrue()
        Assertions.assertThat(result.getOrNull()).isEqualTo(expected)
    }

    @Test
    fun `when the methods response is null, then return error`() = testBlocking {
        whenever(shippingMethodsStore.fetchShippingMethods(siteModel)).doReturn(WooResult(null))

        val result = sut.invoke()

        Assertions.assertThat(result.isSuccess).isFalse()
        Assertions.assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `when the methods response fails, then return error`() = testBlocking {
        val error = WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)
        whenever(shippingMethodsStore.fetchShippingMethods(siteModel)).doReturn(WooResult(error))

        val result = sut.invoke()

        Assertions.assertThat(result.isSuccess).isFalse()
        Assertions.assertThat(result.isFailure).isTrue()
    }
}
