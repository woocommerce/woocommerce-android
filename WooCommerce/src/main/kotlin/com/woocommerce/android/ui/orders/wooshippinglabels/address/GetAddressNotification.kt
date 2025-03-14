package com.woocommerce.android.ui.orders.wooshippinglabels.address

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingAddresses
import javax.inject.Inject

class GetAddressNotification @Inject constructor(private val addressValidationHelper: AddressValidationHelper) {

    operator fun invoke(
        addresses: WooShippingAddresses,
        previousNotification: AddressNotification? = null
    ): AddressNotification? {
        return when {
            addresses.shipFrom.isVerified.not() -> {
                AddressNotification(
                    isSuccess = false,
                    message = R.string.woo_shipping_address_notification_origin_unverified,
                    isDestinationNotification = false
                )
            }

            addressValidationHelper.isMissingDestinationAddress(addresses.shipTo.address) -> {
                AddressNotification(
                    isSuccess = false,
                    message = R.string.woo_shipping_address_notification_destination_missing,
                    isDestinationNotification = true
                )
            }

            addresses.shipTo.isVerified.not() -> {
                AddressNotification(
                    isSuccess = false,
                    message = R.string.woo_shipping_address_notification_destination_unverified,
                    isDestinationNotification = true
                )
            }

            addresses.shipTo.isVerified &&
                previousNotification?.let { it.isSuccess.not() && it.isDestinationNotification } == true -> {
                AddressNotification(
                    isSuccess = true,
                    message = R.string.woo_shipping_address_notification_destination_verified,
                    expireAfter = SUCCESS_EXPIRE_TIME,
                    isDestinationNotification = true
                )
            }

            addresses.shipFrom.isVerified &&
                previousNotification?.let { it.isSuccess.not() && it.isDestinationNotification.not() } == true -> {
                AddressNotification(
                    isSuccess = true,
                    message = R.string.woo_shipping_address_notification_origin_verified,
                    expireAfter = SUCCESS_EXPIRE_TIME,
                    isDestinationNotification = false
                )
            }

            else -> null
        }
    }

    companion object {
        private const val SUCCESS_EXPIRE_TIME = 2_000L
    }
}

data class AddressNotification(
    val isSuccess: Boolean,
    @StringRes val message: Int,
    val expireAfter: Long? = null,
    private val timestamp: Long = System.currentTimeMillis(),
    val isDestinationNotification: Boolean = false,
) {
    fun isExpired(): Boolean = expireAfter?.let {
        timestamp + it < System.currentTimeMillis()
    } == true
}
