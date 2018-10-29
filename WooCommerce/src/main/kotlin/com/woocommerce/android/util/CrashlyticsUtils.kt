package com.woocommerce.android.util

import android.content.Context
import com.crashlytics.android.Crashlytics
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.WooLog.T
import io.fabric.sdk.android.Fabric
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.util.AppLog as WordPressAppLog

object CrashlyticsUtils {
    private const val TAG_KEY = "tag"
    private const val MESSAGE_KEY = "message"

    private fun isCrashlyticsAllowed(): Boolean {
        return AnalyticsTracker.sendUsageStats && !BuildConfig.DEBUG
    }

    fun initCrashlytics(context: Context, account: AccountModel?) {
        if (!isCrashlyticsAllowed()) { return }

        Fabric.with(context, Crashlytics())
        initAccount(account)

        // Send logs for app events through to Crashlytics
        WooLog.addListener { tag, logLevel, message ->
            CrashlyticsUtils.log("$logLevel/${WooLog.TAG}-$tag: $message")
        }

        // Send logs for library events (FluxC, Login, utils) through to Crashlytics
        WordPressAppLog.addListener { tag, logLevel, message ->
            CrashlyticsUtils.log("$logLevel/${WordPressAppLog.TAG}-$tag: $message")
        }
    }

    fun initAccount(account: AccountModel?) {
        if (!isCrashlyticsAllowed()) { return }

        Crashlytics.setUserName(account?.userName)
        Crashlytics.setUserEmail(account?.email)
        Crashlytics.setUserIdentifier(account?.userId.toString())
    }

    fun resetAccount() {
        initAccount(null)
    }

    fun logException(tr: Throwable, tag: T? = null, message: String? = null) {
        if (!Fabric.isInitialized() || !isCrashlyticsAllowed()) { return }

        tag?.let { Crashlytics.setString(TAG_KEY, it.name) }

        message?.let { Crashlytics.setString(MESSAGE_KEY, it) }

        Crashlytics.logException(tr)
    }

    fun log(message: String) {
        if (!Fabric.isInitialized() || !isCrashlyticsAllowed()) { return }

        Crashlytics.log(message)
    }
}
