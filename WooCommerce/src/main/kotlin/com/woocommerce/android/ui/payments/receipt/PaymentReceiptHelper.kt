package com.woocommerce.android.ui.payments.receipt

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.payments.cardreader.onboarding.WCPAY_RECEIPTS_SENDING_SUPPORT_VERSION
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class PaymentReceiptHelper @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    fun storeReceiptUrl(orderId: Long, receiptUrl: String) {
        selectedSite.get().let {
            appPrefsWrapper.setReceiptUrl(it.id, it.siteId, it.selfHostedSiteId, orderId, receiptUrl)
        }
    }

    fun getReceiptUrl(orderId: Long) = selectedSite.get().let {
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
}
