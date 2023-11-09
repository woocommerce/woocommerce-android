package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DetermineMultipleLinesContextTest : BaseUnitTest() {

    private val location: GetLocations = mock()
    private val resourceProvider: ResourceProvider = mock()
    @Test
    fun `when order has multiple shipping lines, then return proper multiple lines context`() {
        whenever(location.invoke(any(), any())).thenReturn(
            Pair(
                Location(code = LocationCode(), name = ""),
                AmbiguousLocation.EMPTY
            )
        )
        whenever(resourceProvider.getString(any())).thenReturn("")
        whenever(resourceProvider.getString(any(), any())).thenReturn("")
        val sut = DetermineMultipleLinesContext(resourceProvider)

        val result = sut.invoke(
            OrderMapper(location).toAppModel(OrderTestUtils.generateOrderWithMultipleShippingLines())
        )

        assertThat(result).isInstanceOf(OrderCreateEditViewModel.MultipleLinesContext.Warning::class.java)
    }

    @Test
    fun `when order does not have multiple shipping lines, then return None MultipleLinesContext`() {
        whenever(location.invoke(any(), any())).thenReturn(
            Pair(
                Location(code = LocationCode(), name = ""),
                AmbiguousLocation.EMPTY
            )
        )
        val sut = DetermineMultipleLinesContext(resourceProvider)

        val result = sut.invoke(
            OrderMapper(location).toAppModel(OrderTestUtils.generateOrder())
        )

        assertThat(result).isInstanceOf(OrderCreateEditViewModel.MultipleLinesContext.None::class.java)
    }
}
