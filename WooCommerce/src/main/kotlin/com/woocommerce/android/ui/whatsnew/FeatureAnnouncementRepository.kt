package com.woocommerce.android.ui.whatsnew

import com.woocommerce.android.model.FeatureAnnouncement
import com.woocommerce.android.model.FeatureAnnouncementItem

object FeatureAnnouncementRepository {
    val exampleAnnouncement = FeatureAnnouncement(
        appVersionName = "14.2",
        announcementVersion = 1337,
        minimumAppVersion = "14.2",
        maximumAppVersion = "14.3",
        appVersionTargets = listOf("alpha-centauri-1", "alpha-centauri-2"),
        detailsUrl = "https://woocommerce.com/",
        features = listOf(
            FeatureAnnouncementItem(
                title = "Super Publishing",
                subtitle = "Super Publishing is here! Publish using the power of your mind.",
                iconBase64 = "",
                iconUrl = "https://s0.wordpress.com/i/store/mobile/plans-personal.png"
            ),
            FeatureAnnouncementItem(
                title = "Amazing Feature",
                subtitle = "That's right! They are right in the app! They require pets right now.",
                iconBase64 = "",
                iconUrl = "https://s0.wordpress.com/i/store/mobile/plans-premium.png"
            ),
            FeatureAnnouncementItem(
                title = "We like long feature announcements that why this one is going to be extra long",
                subtitle = "Super Publishing is here! Publish using the power of your mind.",
                iconBase64 = "",
                iconUrl = "https://s0.wordpress.com/i/store/mobile/plans-business.png"
            )
        )
    )
}
