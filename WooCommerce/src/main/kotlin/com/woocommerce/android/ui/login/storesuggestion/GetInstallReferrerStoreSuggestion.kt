package com.woocommerce.android.ui.login.storesuggestion

import android.os.Parcelable
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.BuildConfigWrapper
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GetInstallReferrerStoreSuggestion @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val installReferrerSource: InstallReferrerSource,
    private val installReferrerParser: InstallReferrerParser,
    private val registrableDomainResolver: RegistrableDomainResolver,
    private val currentTimeProvider: CurrentTimeProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend operator fun invoke(): StoreSuggestion? {
        if (!buildConfigWrapper.debug) return null
        if (appPrefsWrapper.isInstallReferrerChecked) return null
        appPrefsWrapper.isInstallReferrerChecked = true

        return withContext(NonCancellable + dispatchers.io) {
            val outcome = evaluate(installReferrerSource.fetch())
            track(outcome)
            (outcome.parsed?.siteHost as? SiteHost.Valid)
                ?.takeIf { outcome.status == Status.VALID }
                ?.let { (host) ->
                    StoreSuggestion(
                        siteUrl = "https://$host",
                        host = host,
                        registrableDomain = registrableDomainResolver.resolve(host),
                    )
                }
        }
    }

    private fun evaluate(result: InstallReferrerResult): Outcome = when (result) {
        InstallReferrerResult.Unavailable -> Outcome(Status.UNAVAILABLE)
        InstallReferrerResult.Error -> Outcome(Status.ERROR)
        is InstallReferrerResult.Available -> when {
            result.referrer.isBlank() -> Outcome(Status.NO_REFERRER)
            result.referrer.length > MAX_REFERRER_LENGTH -> Outcome(Status.TOO_LONG)
            else -> installReferrerParser.parse(result.referrer).let { Outcome(statusFor(it.siteHost, result), it) }
        }
    }

    private fun statusFor(siteHost: SiteHost, result: InstallReferrerResult.Available): Status = when (siteHost) {
        SiteHost.Missing -> Status.NO_SITE_HOST
        SiteHost.Invalid -> Status.INVALID_SITE_HOST
        is SiteHost.Valid -> if (isStale(result)) Status.STALE else Status.VALID
    }

    private fun isStale(result: InstallReferrerResult.Available): Boolean {
        val click = result.clickTimestampSeconds
        val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTimeProvider.currentDate().time)
        return click <= 0 ||
            click - nowSeconds > MAX_CLOCK_SKEW_SECONDS ||
            nowSeconds - click > MAX_REFERRER_AGE_SECONDS ||
            result.installBeginTimestampSeconds < click
    }

    private fun track(outcome: Outcome) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.LOGIN_INSTALL_REFERRER_CHECKED,
            buildMap {
                put(AnalyticsTracker.KEY_RESULT, outcome.status.value)
                put(AnalyticsTracker.KEY_HAS_SITE_HOST, outcome.parsed?.siteHost?.let { it != SiteHost.Missing } == true)
                outcome.parsed?.campaign?.let {
                    put(AnalyticsTracker.KEY_UTM_CAMPAIGN, it.takeIf { it in KNOWN_CAMPAIGNS } ?: OTHER_CAMPAIGN)
                }
            }
        )
    }

    private data class Outcome(val status: Status, val parsed: ParsedInstallReferrer? = null)

    private enum class Status(val value: String) {
        VALID("valid"),
        NO_REFERRER("no_referrer"),
        TOO_LONG("too_long"),
        NO_SITE_HOST("no_site_host"),
        INVALID_SITE_HOST("invalid_site_host"),
        STALE("stale"),
        UNAVAILABLE("unavailable"),
        ERROR("error"),
    }

    private companion object {
        const val MAX_REFERRER_LENGTH = 2_048
        val MAX_REFERRER_AGE_SECONDS = TimeUnit.HOURS.toSeconds(24)
        val MAX_CLOCK_SKEW_SECONDS = TimeUnit.MINUTES.toSeconds(5)
        val KNOWN_CAMPAIGNS = setOf("deeplinks_promote_app", "deeplinks_payments", "deeplinks_orders_details")
        const val OTHER_CAMPAIGN = "other"
    }
}

class RegistrableDomainResolver @Inject constructor() {
    fun resolve(host: String): String? = try {
        "https://$host".toHttpUrlOrNull()?.topPrivateDomain()
    } catch (_: IllegalStateException) {
        null
    }
}

@Parcelize
data class StoreSuggestion(
    val siteUrl: String,
    val host: String,
    val registrableDomain: String?,
) : Parcelable
