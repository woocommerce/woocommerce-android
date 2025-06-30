package com.woocommerce.android.ui.prefs

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.automattic.about.model.AboutConfig
import com.automattic.about.model.AboutFooterConfig
import com.automattic.about.model.AnalyticsConfig
import com.automattic.about.model.AutomatticConfig
import com.automattic.about.model.HeaderConfig
import com.automattic.about.model.LegalConfig
import com.automattic.about.model.RateUsConfig
import com.automattic.about.model.ShareConfig
import com.automattic.about.model.SocialsConfig
import com.automattic.about.model.WorkWithUsConfig
import com.woocommerce.android.AppConstants
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.SETTINGS_ABOUT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class AboutConfigBuilder @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun createAboutConfig(activity: AppCompatActivity) =
        AboutConfig(
            headerConfig = HeaderConfig.fromContext(activity),
            rateUsConfig = RateUsConfig.fromContext(activity),
            shareConfigFactory = { createShareConfig(activity) },
            socialsConfig = SocialsConfig(
                instagramUsername = AppConstants.INSTAGRAM_USERNAME,
                xUsername = AppConstants.X_USERNAME,
            ),
            legalConfig = LegalConfig(
                tosUrl = AppUrls.AUTOMATTIC_TOS,
                privacyPolicyUrl = AppUrls.AUTOMATTIC_PRIVACY_POLICY,
                californiaPrivacyNoticeUrl = AppUrls.AUTOMATTIC_PRIVACY_POLICY_CA,
            ),
            automatticConfig = AutomatticConfig(true),
            workWithUsConfig = WorkWithUsConfig(
                title = activity.getString(R.string.about_automattic_work_with_us_item_title),
                subtitle = activity.getString(R.string.about_automattic_work_with_us_item_subtitle),
                url = AppUrls.AUTOMATTIC_HIRING
            ),
            aboutFooterConfig = AboutFooterConfig(true),
            analyticsConfig = createAnalyticsConfig(),
            onDismiss = { activity.finish() },
        )

    private fun createShareConfig(context: Context) = ShareConfig(
        subject = context.getString(R.string.settings_about_recommend_app_subject),
        message = context.getString(
            R.string.settings_about_recommend_app_message,
            AppUrls.PLAY_STORE_APP_PREFIX + context.packageName
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
