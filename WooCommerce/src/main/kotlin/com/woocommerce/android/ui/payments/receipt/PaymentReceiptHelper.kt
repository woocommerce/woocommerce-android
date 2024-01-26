package com.woocommerce.android.ui.payments.receipt

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class PaymentReceiptHelper @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val orderStore: WCOrderStore,
) {
    fun storeReceiptUrl(orderId: Long, receiptUrl: String) {
        selectedSite.get().let {
            appPrefsWrapper.setReceiptUrl(it.id, it.siteId, it.selfHostedSiteId, orderId, receiptUrl)
        }
    }

    suspend fun getReceiptUrl(orderId: Long): Result<String> =
        if (isWCCanGenerateReceipts()) {
            val fetchingResult = orderStore.fetchOrdersReceipt(
                site = selectedSite.get(),
                orderId = orderId,
                expirationDays = RECEIPT_EXPIRATION_DAYS,
            )

            val result = fetchingResult.result
            if (fetchingResult.isError || result == null) {
                Result.failure(Exception(fetchingResult.error.message))
            } else {
                Result.success(result.receiptUrl)
            }
        } else {
            Result.success(getReceiptUrlFromAppPrefs(orderId))
        }

    suspend fun isReceiptAvailable(orderId: Long) =
        when {
            isWCCanGenerateReceipts() -> true
            getReceiptUrlFromAppPrefs(orderId).isNotEmpty() -> true
            else -> false
        }

    private fun getReceiptUrlFromAppPrefs(orderId: Long) = selectedSite.get().let {
        appPrefsWrapper.getReceiptUrl(it.id, it.siteId, it.selfHostedSiteId, orderId)
    }

    fun isPluginCanSendReceipt(site: SiteModel): Boolean {
        val preferredPlugin = appPrefsWrapper.getCardReaderPreferredPlugin(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
        return if (preferredPlugin == null || preferredPlugin != PluginType.WOOCOMMERCE_PAYMENTS) {
            false
        } else {
            val pluginVersion = appPrefsWrapper.getCardReaderPreferredPluginVersion(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId,
                preferredPlugin,
            ) ?: return false

            pluginVersion.semverCompareTo(WCPAY_RECEIPTS_SENDING_SUPPORT_VERSION) >= 0
        }
    }

    private suspend fun isWCCanGenerateReceipts() =
        withContext(Dispatchers.IO) {
            val currentWooCoreVersion =
                wooCommerceStore.getSitePlugin(
                    selectedSite.get(),
                    WooCommerceStore.WooPlugin.WOO_CORE
                )?.version ?: return@withContext false

            currentWooCoreVersion.semverCompareTo(WC_CAN_GENERATE_RECEIPTS_VERSION) >= 0
        }

    private companion object {
        const val WCPAY_RECEIPTS_SENDING_SUPPORT_VERSION = "4.0.0"
        const val WC_CAN_GENERATE_RECEIPTS_VERSION = "6.4.0"

        const val RECEIPT_EXPIRATION_DAYS = 365
    }
}
