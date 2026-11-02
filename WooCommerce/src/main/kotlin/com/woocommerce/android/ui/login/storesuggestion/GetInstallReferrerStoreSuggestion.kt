package com.woocommerce.android.ui.login.storesuggestion

import android.os.Parcelable
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GetInstallReferrerStoreSuggestion @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val installReferrerSource: InstallReferrerSource,
    private val installReferrerParser: InstallReferrerParser,
    private val currentTimeProvider: CurrentTimeProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) {
    suspend operator fun invoke(): StoreSuggestion? {
        if (!featureFlagRepository.isEnabled(FeatureFlag.LOGIN_STORE_SUGGESTION)) return null
        if (appPrefsWrapper.isInstallReferrerChecked) return null

        val outcome = withContext(NonCancellable) {
            evaluate(installReferrerSource.fetch()).also {
                appPrefsWrapper.isInstallReferrerChecked = true
                track(it)
            }
        }

        return outcome.parsed?.siteUrl
            ?.takeIf { outcome.status == Status.VALID }
            ?.let { StoreSuggestion(siteUrl = it, displayAddress = it.substringAfter("://")) }
    }

    private fun evaluate(result: InstallReferrerResult): Outcome = when (result) {
        InstallReferrerResult.Unavailable -> Outcome(Status.UNAVAILABLE)
        InstallReferrerResult.Error -> Outcome(Status.ERROR)
        is InstallReferrerResult.Available -> {
            val parsed = installReferrerParser.parse(result.referrer)
            val status = when {
                result.referrer.isBlank() -> Status.NO_REFERRER
                !parsed.hasSiteUrl -> Status.NO_SITE_URL
                parsed.siteUrl == null -> Status.INVALID_SITE_URL
                isStale(result.clickTimestampSeconds) -> Status.STALE
                else -> Status.VALID
            }
            Outcome(status, parsed)
        }
    }

    private fun isStale(clickTimestampSeconds: Long): Boolean {
        if (clickTimestampSeconds <= 0) return true
        val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTimeProvider.currentDate().time)
        return nowSeconds - clickTimestampSeconds > MAX_REFERRER_AGE_SECONDS
    }

    private fun track(outcome: Outcome) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.LOGIN_INSTALL_REFERRER_CHECKED,
            buildMap {
                put(AnalyticsTracker.KEY_RESULT, outcome.status.value)
                put(AnalyticsTracker.KEY_HAS_SITE_URL, outcome.parsed?.hasSiteUrl == true)
                outcome.parsed?.campaign?.let { put(AnalyticsTracker.KEY_UTM_CAMPAIGN, it) }
            }
        )
    }

    private data class Outcome(val status: Status, val parsed: ParsedInstallReferrer? = null)

    private enum class Status(val value: String) {
        VALID("valid"),
        NO_REFERRER("no_referrer"),
        NO_SITE_URL("no_site_url"),
        INVALID_SITE_URL("invalid_site_url"),
        STALE("stale"),
        UNAVAILABLE("unavailable"),
        ERROR("error"),
    }

    private companion object {
        val MAX_REFERRER_AGE_SECONDS = TimeUnit.HOURS.toSeconds(24)
    }
}

@Parcelize
data class StoreSuggestion(
    val siteUrl: String,
    val displayAddress: String,
) : Parcelable
