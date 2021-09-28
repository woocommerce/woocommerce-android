package com.woocommerce.android

import javax.inject.Inject
@Suppress("TooManyFunctions")
class AppPrefsWrapper @Inject constructor() {
    fun getReceiptUrl(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long, orderId: Long) =
        AppPrefs.getReceiptUrl(localSiteId, remoteSiteId, selfHostedSiteId, orderId)

    fun setReceiptUrl(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        orderId: Long,
        url: String
    ) = AppPrefs.setReceiptUrl(localSiteId, remoteSiteId, selfHostedSiteId, orderId, url)

    fun isCardReaderOnboardingCompleted(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long) =
        AppPrefs.isCardReaderOnboardingCompleted(localSiteId, remoteSiteId, selfHostedSiteId)

    fun setCardReaderOnboardingCompleted(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = AppPrefs.setCardReaderOnboardingCompleted(localSiteId, remoteSiteId, selfHostedSiteId)

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
}
