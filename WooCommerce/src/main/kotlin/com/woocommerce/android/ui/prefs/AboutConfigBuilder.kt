package com.woocommerce.android.ui.prefs

import android.content.Context
import com.automattic.about.model.*
import com.woocommerce.android.AppConstants
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_ABOUT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class AboutConfigBuilder @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun createAboutConfig(appCtx: Context) =
        AboutConfig(
            headerConfig = HeaderConfig.fromContext(appCtx),
            rateUsConfig = RateUsConfig.fromContext(appCtx),
            socialsConfig = SocialsConfig(
                twitterUsername = AppConstants.TWITTER_USERNAME,
                instagramUsername = AppConstants.INSTAGRAM_USERNAME,
            ),
            legalConfig = LegalConfig(
                tosUrl = AppUrls.AUTOMATTIC_TOS,
                privacyPolicyUrl = AppUrls.AUTOMATTIC_PRIVACY_POLICY,
                californiaPrivacyNoticeUrl = AppUrls.AUTOMATTIC_PRIVACY_POLICY_CA,
            ),
            shareConfigFactory = { createShareConfig(appCtx) },
            analyticsConfig = createAnalyticsConfig(),
            onDismiss = {
                // noop
            },
        )

    private fun createShareConfig(appCtx: Context) = ShareConfig(
        subject = appCtx.getString(R.string.settings_about_recommend_app_subject),
        message = appCtx.getString(
            R.string.settings_about_recommend_app_message,
            AppUrls.PLAY_STORE_APP_PREFIX + appCtx.packageName
        ),
    )

    private fun createAnalyticsConfig() = AnalyticsConfig(
        trackScreenShown = { name ->
            analyticsTrackerWrapper.trackViewShown("about_screen_$name")
        },
        trackScreenDismissed = {
            // noop
        },
        trackButtonTapped = { buttonName ->
            analyticsTrackerWrapper.track(
                stat = SETTINGS_ABOUT_BUTTON_TAPPED,
                properties = mapOf(
                    "button" to buttonName
                ),
            )
        },
    )
}
