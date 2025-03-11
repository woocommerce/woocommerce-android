package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetAddressNotificationTests : BaseUnitTest() {
    private val sut = GetAddressNotification()

    private val defaultAddresses = WooShippingAddresses(
        shipFrom = OriginShippingAddress.EMPTY.copy(
            id = "1",
            firstName = "John",
            lastName = "Doe",
            address1 = "123 Main St",
            isVerified = true
        ),
        shipTo = DestinationShippingAddress(
            address = Address.EMPTY.copy(
                firstName = "John",
                lastName = "Doe",
                address1 = "123 Main St"
            ),
            isVerified = true
        ),
        originAddresses = emptyList()
    )

    @Test
    fun `when addresses as no issues, then don't display any notification`() {
        val result = sut.invoke(defaultAddresses)
        assertThat(result).isNull()
    }

    @Test
    fun `when addresses as no issues and previous was a destination warning, then display destination success`() {
        val previous = AddressNotification(
            isSuccess = false,
            message = R.string.woo_shipping_address_notification_destination_missing,
            isDestinationNotification = true
        )
        val result = sut.invoke(defaultAddresses, previous)
        assertThat(result).isNotNull
        assertThat(result!!.isSuccess).isTrue
        assertThat(result.isDestinationNotification).isTrue
    }

    @Test
    fun `when addresses as no issues and previous was a origin warning, then display origin success`() {
        val previous = AddressNotification(
            isSuccess = false,
            message = R.string.woo_shipping_address_notification_destination_missing,
            isDestinationNotification = false
        )

        val result = sut.invoke(defaultAddresses, previous)

        assertThat(result).isNotNull
        assertThat(result!!.isSuccess).isTrue
        assertThat(result.isDestinationNotification).isFalse
    }

    @Test
    fun `when shipTo is not verified, then display destination not verified`() {
        val addresses = defaultAddresses.copy(shipTo = defaultAddresses.shipTo.copy(isVerified = false))

        val result = sut.invoke(addresses, null)

        assertThat(result).isNotNull
        assertThat(result!!.isSuccess).isFalse
        assertThat(result.isDestinationNotification).isTrue
        assertThat(result.message).isEqualTo(
            R.string.woo_shipping_address_notification_destination_unverified
        )
    }

    @Test
    fun `when shipFrom is not verified, then display origin not verified`() {
        val addresses = defaultAddresses.copy(shipFrom = defaultAddresses.shipFrom.copy(isVerified = false))

        val result = sut.invoke(addresses, null)

        assertThat(result).isNotNull
        assertThat(result!!.isSuccess).isFalse
        assertThat(result.isDestinationNotification).isFalse
        assertThat(result.message).isEqualTo(
            R.string.woo_shipping_address_notification_origin_unverified
        )
    }

    @Test
    fun `when shipTo is missing, then display destination missing`() {
        val addresses = defaultAddresses.copy(shipTo = DestinationShippingAddress.EMPTY)

        val result = sut.invoke(addresses, null)

        assertThat(result).isNotNull
        assertThat(result!!.isSuccess).isFalse
        assertThat(result.isDestinationNotification).isTrue
        assertThat(result.message).isEqualTo(
            R.string.woo_shipping_address_notification_destination_missing
        )
    }

    @Test
    fun `testing both addresses with issues flow`() {
        var addresses = defaultAddresses.copy(
            shipTo = defaultAddresses.shipTo.copy(isVerified = false),
            shipFrom = defaultAddresses.shipFrom.copy(isVerified = false)
        )

        // When address have issues, then display origin warnings first

        var result = sut.invoke(addresses, null)

        assertThat(result).isNotNull
        assertThat(result!!.isSuccess).isFalse
        assertThat(result.isDestinationNotification).isFalse
        assertThat(result.message).isEqualTo(
            R.string.woo_shipping_address_notification_origin_unverified
        )

        // Fix origin issue
        addresses = defaultAddresses.copy(
            shipTo = defaultAddresses.shipTo.copy(isVerified = false)
        )

        result = sut.invoke(addresses, result)

        assertThat(result).isNotNull
        assertThat(result!!.isSuccess).isFalse
        assertThat(result.isDestinationNotification).isTrue
        assertThat(result.message).isEqualTo(
            R.string.woo_shipping_address_notification_destination_unverified
        )

        // Fix destination issue
        addresses = defaultAddresses
        result = sut.invoke(addresses, result)

        assertThat(result).isNotNull
        assertThat(result!!.isSuccess).isTrue
        assertThat(result.isDestinationNotification).isTrue
        assertThat(result.message).isEqualTo(
            R.string.woo_shipping_address_notification_destination_verified
        )
    }
}
