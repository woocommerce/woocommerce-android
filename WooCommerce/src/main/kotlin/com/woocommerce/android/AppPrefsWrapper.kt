package com.woocommerce.android

import com.woocommerce.android.model.OrderId
import com.woocommerce.android.ui.prefs.cardreader.onboarding.PluginType
import javax.inject.Inject

class AppPrefsWrapper @Inject constructor() {
    fun getReceiptUrl(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long, orderId: OrderId) =
        AppPrefs.getReceiptUrl(localSiteId, remoteSiteId, selfHostedSiteId, orderId)

    fun setReceiptUrl(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        orderId: OrderId,
        url: String
    ) = AppPrefs.setReceiptUrl(localSiteId, remoteSiteId, selfHostedSiteId, orderId, url)

    fun isCardReaderOnboardingCompleted(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long) =
        AppPrefs.isCardReaderOnboardingCompleted(localSiteId, remoteSiteId, selfHostedSiteId)

    fun setCardReaderOnboardingCompleted(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        pluginType: PluginType?
    ) = AppPrefs.setCardReaderOnboardingCompleted(localSiteId, remoteSiteId, selfHostedSiteId, pluginType)

    fun setLastConnectedCardReaderId(readerId: String) = AppPrefs.setLastConnectedCardReaderId(readerId)

    fun getLastConnectedCardReaderId() = AppPrefs.getLastConnectedCardReaderId()

    fun removeLastConnectedCardReaderId() = AppPrefs.removeLastConnectedCardReaderId()

    fun isOrderNotificationsEnabled() = AppPrefs.isOrderNotificationsEnabled()

    fun isReviewNotificationsEnabled() = AppPrefs.isReviewNotificationsEnabled()

    fun isOrderNotificationsChaChingEnabled() = AppPrefs.isOrderNotificationsChaChingEnabled()

    fun hasUnseenReviews() = AppPrefs.getHasUnseenReviews()

    fun setHasUnseenReviews(hasUnseen: Boolean) {
        AppPrefs.setHasUnseenReviews(hasUnseen)
    }

    fun getJetpackBenefitsDismissalDate(): Long {
        return AppPrefs.getJetpackBenefitsDismissalDate()
    }

    fun recordJetpackBenefitsDismissal() {
        AppPrefs.recordJetpackBenefitsDismissal()
    }

    fun setOrderFilters(selectedSiteId: Int, filterCategory: String, filterValue: String) {
        AppPrefs.setOrderFilters(selectedSiteId, filterCategory, filterValue)
    }

    fun getOrderFilters(selectedSiteId: Int, filterCategory: String) =
        AppPrefs.getOrderFilters(selectedSiteId, filterCategory)

    fun setOrderFilterCustomDateRange(selectedSiteId: Int, startDateMillis: Long, endDateMillis: Long) {
        AppPrefs.setOrderFilterCustomDateRange(selectedSiteId, startDateMillis, endDateMillis)
    }

    fun getOrderFilterCustomDateRange(selectedSiteId: Int): Pair<Long, Long> =
        AppPrefs.getOrderFilterCustomDateRange(selectedSiteId)

    fun setV4StatsSupported(supported: Boolean) {
        AppPrefs.setV4StatsSupported(supported)
    }
}
