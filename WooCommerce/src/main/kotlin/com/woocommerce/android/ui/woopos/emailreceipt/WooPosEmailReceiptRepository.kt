package com.woocommerce.android.ui.woopos.emailreceipt

import android.util.Patterns
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class WooPosEmailReceiptRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val provideEmailPattern: WooPosProvideEmailPattern,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion
) {
    suspend fun sendReceiptByEmail(orderId: Long, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext if (posReceiptsAreEnabled()) {
            sendPOSReceipt(orderId, email)
        } else {
            sendLegacyReceipt(orderId, email)
        }
    }

    suspend fun getBillingEmail(orderId: Long): String? = withContext(Dispatchers.IO) {
        orderStore.getOrderByIdAndSite(orderId, selectedSite.get())
            ?.billingEmail
            ?.takeIf { it.isNotBlank() }
    }

    fun isEmailValid(email: String): Boolean = provideEmailPattern().matcher(email).matches()

    fun posReceiptsAreEnabled(): Boolean = isWooCoreSupportsPOSReceipts()

    private suspend fun sendPOSReceipt(orderId: Long, email: String): Result<Unit> {
        val templateId = if (supportsAutoTemplateSelection()) null else TEMPLATE_ID_POS_COMPLETED_ORDER
        val sendOrderResult = orderStore.sendOrderPOSSpecificReceipt(
            selectedSite.get(),
            orderId,
            email,
            forceEmailUpdate = true,
            templateId = templateId
        )
        return if (sendOrderResult.isError) {
            Result.failure(Exception("Failed to send order receipt"))
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun sendLegacyReceipt(orderId: Long, email: String): Result<Unit> {
        val updateResult = orderStore.updateOrderBillingEmail(selectedSite.get(), orderId, email)
        if (updateResult.isError) {
            return Result.failure(Exception("Failed to update order with email"))
        }

        val sendOrderResult = orderStore.sendOrderReceipt(selectedSite.get(), orderId)
        return if (sendOrderResult.isError) {
            Result.failure(Exception("Failed to send order receipt"))
        } else {
            Result.success(Unit)
        }
    }

    private fun isWooCoreSupportsPOSReceipts(): Boolean {
        val wooCoreVersion = getWooCoreVersion() ?: return false
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_POS_RECEIPTS) >= 0
    }

    private fun supportsAutoTemplateSelection(): Boolean {
        val wooCoreVersion = getWooCoreVersion() ?: return false
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_AUTO_TEMPLATE) >= 0
    }

    private companion object {
        const val WC_VERSION_SUPPORTS_POS_RECEIPTS = "10.0.0"
        const val WC_VERSION_SUPPORTS_AUTO_TEMPLATE = "10.7.0"
        const val TEMPLATE_ID_POS_COMPLETED_ORDER = "customer_pos_completed_order"
    }

    class WooPosProvideEmailPattern @Inject constructor() {
        operator fun invoke() = Patterns.EMAIL_ADDRESS
    }
}
