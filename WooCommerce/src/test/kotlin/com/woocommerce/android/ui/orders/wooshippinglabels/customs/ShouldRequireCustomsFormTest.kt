package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShouldRequireCustomsFormTest {

    private lateinit var shouldRequireCustomsForm: ShouldRequireCustomsForm

    @Before
    fun setup() {
        shouldRequireCustomsForm = ShouldRequireCustomsForm()
    }

    @Test
    fun `should return true for different countries`() {
        val addressData = WooShippingAddresses(
            shipFrom = OriginShippingAddress.EMPTY.copy(country = "US", state = "CA"),
            shipTo = DestinationShippingAddress(
                Address.EMPTY.copy(
                    country = Location.EMPTY.copy(code = "CA"),
                    state = AmbiguousLocation.Defined(Location.EMPTY.copy(code = "ON"))
                ),
                false
            ),
            originAddresses = emptyList()
        )
        assertTrue(shouldRequireCustomsForm(addressData))
    }

    @Test
    fun `should return false for same country`() {
        val addressData = WooShippingAddresses(
            shipFrom = OriginShippingAddress.EMPTY.copy(country = "US", state = "CA"),
            shipTo = DestinationShippingAddress(
                Address.EMPTY.copy(
                    country = Location.EMPTY.copy(code = "US"),
                    state = AmbiguousLocation.Defined(Location.EMPTY.copy(code = "NY"))
                ),
                false
            ),
            originAddresses = emptyList()
        )
        assertFalse(shouldRequireCustomsForm(addressData))
    }

    @Test
    fun `should return true for military origin address`() {
        val addressData = WooShippingAddresses(
            shipFrom = OriginShippingAddress.EMPTY.copy(country = "US", state = "AA"),
            shipTo = DestinationShippingAddress(
                Address.EMPTY.copy(
                    country = Location.EMPTY.copy(code = "US"),
                    state = AmbiguousLocation.Defined(Location.EMPTY.copy(code = "NY"))
                ),
                false
            ),
            originAddresses = emptyList()
        )
        assertTrue(shouldRequireCustomsForm(addressData))
    }

    @Test
    fun `should return true for military shipping address`() {
        val addressData = WooShippingAddresses(
            shipFrom = OriginShippingAddress.EMPTY.copy(country = "US", state = "CA"),
            shipTo = DestinationShippingAddress(
                Address.EMPTY.copy(
                    country = Location.EMPTY.copy(code = "US"),
                    state = AmbiguousLocation.Defined(Location.EMPTY.copy(code = "AE"))
                ),
                false
            ),
            originAddresses = emptyList()
        )
        assertTrue(shouldRequireCustomsForm(addressData))
    }

    @Test
    fun `should return false for no military addresses`() {
        val addressData = WooShippingAddresses(
            shipFrom = OriginShippingAddress.EMPTY.copy(country = "US", state = "CA"),
            shipTo = DestinationShippingAddress(
                Address.EMPTY.copy(
                    country = Location.EMPTY.copy(code = "US"),
                    state = AmbiguousLocation.Defined(Location.EMPTY.copy(code = "NY"))
                ),
                false
            ),
            originAddresses = emptyList()
        )
        assertFalse(shouldRequireCustomsForm(addressData))
    }

    @Test
    fun `should return false for empty destination addresses`() {
        val addressData = WooShippingAddresses(
            shipFrom = OriginShippingAddress.EMPTY.copy(country = "US", state = "CA"),
            shipTo = DestinationShippingAddress(
                Address.EMPTY,
                false
            ),
            originAddresses = emptyList()
        )
        assertFalse(shouldRequireCustomsForm(addressData))
    }
}
