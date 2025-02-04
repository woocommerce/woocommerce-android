package org.wordpress.android.fluxc.model.shippinglabels

import com.google.gson.annotations.SerializedName

data class WCShippingAccountSettings(
    val isCreatingLabelsEnabled: Boolean,
    val isEmailReceiptEnabled: Boolean,
    val paperSize: WCShippingLabelPaperSize,
    val canEditSettings: Boolean,
    val canManagePayments: Boolean,
    val storeOwnerName: String,
    val storeOwnerUserName: String,
    val storeOwnerWpcomUserName: String,
    val storeOwnerWpcomEmail: String,
    val selectedPaymentMethodId: Int?,
    val paymentMethods: List<WCPaymentMethod>,
    val lastUsedBoxId: String?
)

data class WCPaymentMethod(
    @SerializedName("payment_method_id")
    val paymentMethodId: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("card_type")
    val cardType: String,
    @SerializedName("card_digits")
    val cardDigits: String,
    @SerializedName("expiry")
    val expiry: String
)
