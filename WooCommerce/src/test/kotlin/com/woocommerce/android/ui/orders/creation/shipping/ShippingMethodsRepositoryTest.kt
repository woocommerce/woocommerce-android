package com.woocommerce.android.ui.orders.creation.shipping

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
import org.wordpress.android.fluxc.model.WCShippingMethod
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCShippingMethodsStore

@OptIn(ExperimentalCoroutinesApi::class)
class ShippingMethodsRepositoryTest : BaseUnitTest() {

    private val selectedSite: SelectedSite = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val shippingMethodsStore: WCShippingMethodsStore = mock()

    private val sut = ShippingMethodsRepository(
        selectedSite = selectedSite,
        dispatchers = coroutinesTestRule.testDispatchers,
        resourceProvider = resourceProvider,
        shippingMethodsStore = shippingMethodsStore
    )

    @Test
    fun `when shipping requests succeed then a success response is returned`() = testBlocking {
        val siteModel = SiteModel()
        val fetchResult = List(3) { i ->
            WCShippingMethod(
                id = "id$i",
                title = "title$i",
            )
        }
        whenever(shippingMethodsStore.fetchShippingMethods(siteModel)).doReturn(WooResult(fetchResult))

        val result = sut.fetchShippingMethodsAndSaveResults(siteModel)
        assertThat(result.isError).isFalse()
        assertThat(result.model).isNotNull
        assertThat(result.model?.size).isEqualTo(fetchResult.size)
    }

    @Test
    fun `when shipping requests succeed but model is null then an error response is returned`() = testBlocking {
        val siteModel = SiteModel()
        val fetchResult = null
        whenever(shippingMethodsStore.fetchShippingMethods(siteModel)).doReturn(WooResult(fetchResult))

        val result = sut.fetchShippingMethodsAndSaveResults(siteModel)
        assertThat(result.isError).isTrue()
        assertThat(result.model).isNull()
    }

    @Test
    fun `when shipping requests fails then an error response is returned`() = testBlocking {
        val siteModel = SiteModel()
        val fetchResult = WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)
        whenever(shippingMethodsStore.fetchShippingMethods(siteModel)).doReturn(WooResult(fetchResult))

        val result = sut.fetchShippingMethodsAndSaveResults(siteModel)
        assertThat(result.isError).isTrue()
        assertThat(result.model).isNull()
    }
}
