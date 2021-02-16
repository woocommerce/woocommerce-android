package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingAccountSettings
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.Date

@Parcelize
data class ShippingAccountSettings(
    val storeOwnerDetails: StoreOwnerDetails,
    val isEmailReceiptEnabled: Boolean,
    val canManagePayments: Boolean,
    val selectedPaymentId: Int?,
    val paymentMethods: List<PaymentMethod>,
    val lastUsedBoxId: String?
) : Parcelable

@Parcelize
data class StoreOwnerDetails(
    val wpcomEmail: String,
    val wpcomUserName: String,
    val userName: String,
    val name: String
) : Parcelable

@Parcelize
data class PaymentMethod(
    val id: Int,
    val name: String,
    val cardType: String,
    val cardDigits: String,
    val expirationDate: Date
) : Parcelable

fun WCShippingAccountSettings.toAppModel(): ShippingAccountSettings {
    return ShippingAccountSettings(
        storeOwnerDetails = StoreOwnerDetails(
            wpcomEmail = storeOwnerWpcomEmail,
            wpcomUserName = storeOwnerUserName,
            name = storeOwnerName,
            userName = storeOwnerUserName
        ),
        isEmailReceiptEnabled = isEmailReceiptEnabled,
        canManagePayments = canManagePayments,
        selectedPaymentId = selectedPaymentMethodId,
        paymentMethods = paymentMethods.map {
            PaymentMethod(
                id = it.paymentMethodId,
                name = it.name,
                cardType = it.cardType,
                cardDigits = it.cardDigits,
                expirationDate = DateUtils.getDateFromString(it.expiry)
            )
        },
        lastUsedBoxId = lastUsedBoxId
    )
}
