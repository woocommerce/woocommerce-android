package com.woocommerce.android.util

import android.content.Context
import com.crashlytics.android.Crashlytics
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.util.WooLog.T
import io.fabric.sdk.android.Fabric
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel

object CrashlyticsUtils {
    private const val TAG_KEY = "tag"
    private const val MESSAGE_KEY = "message"
    private const val SITE_ID_KEY = "site_id"
    private const val SITE_URL_KEY = "site_url"

    private fun isCrashlyticsEnabled() = AppPrefs.isCrashReportingEnabled()

    fun initCrashlytics(context: Context, account: AccountModel?, site: SiteModel?) {
        if (!isCrashlyticsEnabled()) { return }

        Fabric.with(context, Crashlytics())
        initAccount(account)
        initSite(site)

        // Send logs for app events through to Crashlytics
        WooLog.addListener { tag, logLevel, message ->
            CrashlyticsUtils.log("$logLevel/${WooLog.TAG}-$tag: $message")
        }
    }

    fun initAccount(account: AccountModel?) {
        if (!isCrashlyticsEnabled()) { return }

        Crashlytics.setUserName(account?.userName)
        Crashlytics.setUserEmail(account?.email)
        Crashlytics.setUserIdentifier(account?.userId.toString())
    }

    fun initSite(site: SiteModel?) {
        if (!isCrashlyticsEnabled()) { return }

        Crashlytics.setLong(SITE_ID_KEY, site?.siteId ?: 0)
        Crashlytics.setString(SITE_URL_KEY, site?.url)
    }

    fun resetAccountAndSite() {
        initAccount(null)
        initSite(null)
    }

    fun logException(tr: Throwable, tag: T? = null, message: String? = null) {
        if (!Fabric.isInitialized() || !isCrashlyticsEnabled()) { return }

        tag?.let { Crashlytics.setString(TAG_KEY, it.name) }

        message?.let { Crashlytics.setString(MESSAGE_KEY, it) }

        Crashlytics.logException(tr)
    }

    fun log(message: String) {
        if (!Fabric.isInitialized() || !isCrashlyticsEnabled()) { return }

        Crashlytics.log(message)
    }
}
