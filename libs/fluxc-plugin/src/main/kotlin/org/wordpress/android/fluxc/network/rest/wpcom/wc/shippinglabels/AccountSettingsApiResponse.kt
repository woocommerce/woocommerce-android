package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.shippinglabels.WCPaymentMethod

data class AccountSettingsApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("formData") val formData: FormData,
    @SerializedName("formMeta") val formMeta: FormMeta,
    @SerializedName("userMeta") val userMeta: UserMeta
) {
    data class FormData(
        @SerializedName("enabled") val isCreatingLabelsEnabled: Boolean,
        @SerializedName("selected_payment_method_id") val selectedPaymentId: Int?,
        @SerializedName("paper_size") val paperSize: String,
        @SerializedName("email_receipts") val isPaymentReceiptEnabled: Boolean
    )

    data class FormMeta(
        @SerializedName("can_edit_settings") val canEditSettings: Boolean,
        @SerializedName("can_manage_payments") val canManagePayments: Boolean,
        @SerializedName("master_user_name") val storeOwnerName: String,
        @SerializedName("master_user_login") val storeOwnerUserName: String,
        @SerializedName("master_user_wpcom_login") val storeOwnerWpcomUserName: String,
        @SerializedName("master_user_email") val storeOwnerWpcomEmail: String,
        @SerializedName("payment_methods") val paymentMethods: List<WCPaymentMethod>?
    )

    data class UserMeta(
        @SerializedName("last_box_id") val lastBoxId: String?
    )
}
