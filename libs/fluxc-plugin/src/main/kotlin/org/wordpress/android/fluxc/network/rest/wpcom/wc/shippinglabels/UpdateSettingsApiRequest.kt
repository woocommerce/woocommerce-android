package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import com.google.gson.annotations.SerializedName

data class UpdateSettingsApiRequest(
    @SerializedName("selected_payment_method_id")
    val selectedPaymentMethodId: Int?,
    @SerializedName("enabled")
    val isCreatingLabelsEnabled: Boolean?,
    @SerializedName("paper_size")
    val paperSize: String?,
    @SerializedName("email_receipts")
    val isEmailReceiptEnabled: Boolean?
)
