package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCShippingMethod
import org.wordpress.android.fluxc.store.WCShippingMethodsStore

@OptIn(ExperimentalCoroutinesApi::class)
class GetShippingMethodsWithOtherValueTest : BaseUnitTest() {

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

    val sut = GetShippingMethodsWithOtherValue(shippingMethodsRepository)

    @Test
    fun `when get shipping succeed then a success response is returned including other`() = testBlocking {
        val fetchResult = List(3) { i ->
            WCShippingMethod(
                id = "id$i",
                title = "title$i",
            )
        }
        whenever(shippingMethodsStore.observeShippingMethods(siteModel)).doReturn(flowOf(fetchResult))
        whenever(resourceProvider.getString(R.string.other)).doReturn("Other")
        whenever(resourceProvider.getString(R.string.na)).doReturn("N/A")

        val result = sut.invoke().first()
        assertThat(result).isNotNull
        assertThat(result.size).isEqualTo(5) // List + Other + N/A
        val other = result.firstOrNull { it.id == ShippingMethodsRepository.OTHER_ID }
        assertThat(other).isNotNull
    }
}
