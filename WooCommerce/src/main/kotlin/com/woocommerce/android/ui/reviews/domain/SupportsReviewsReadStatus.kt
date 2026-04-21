package com.woocommerce.android.ui.reviews.domain

import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import javax.inject.Inject

/**
 * Tells whether the app can currently tell if a review has been read.
 *
 * Review read-status relies on WPCom notifications, which are only usable when the site is
 * Jetpack-connected (otherwise there is no WPCom token) and push notifications are still handled
 * through WPCom (if Woo has taken over push, notifications are no longer delivered to this account
 * and any previously-stored state becomes outdated).
 *
 * When this returns false, callers should skip fetching review notifications, avoid reading or
 * writing review read-status, and hide UI that depends on it (e.g. the unread-reviews filter).
 */
class SupportsReviewsReadStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus,
) {
    suspend operator fun invoke(): Boolean {
        val site = selectedSite.getIfExists() ?: return false
        if (site.connectionType != SiteConnectionType.Jetpack) return false
        val status = pushNotificationRegistrationStatus(site.siteId)
        return !status.isWooRegistered
    }
}
