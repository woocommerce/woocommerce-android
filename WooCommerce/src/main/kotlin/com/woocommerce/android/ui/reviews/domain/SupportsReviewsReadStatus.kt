package com.woocommerce.android.ui.reviews.domain

import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import javax.inject.Inject

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
