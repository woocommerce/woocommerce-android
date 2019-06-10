package com.woocommerce.android.util

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel

import com.automattic.android.tracks.CrashLogging.CrashLogging
import com.automattic.android.tracks.CrashLogging.CrashLoggingDataProvider
import com.automattic.android.tracks.TracksUser
import com.woocommerce.android.BuildConfig
import java.util.Locale

object CrashUtils : CrashLoggingDataProvider {
    private const val TAG_KEY = "tag"
    private const val MESSAGE_KEY = "message"
    private const val SITE_ID_KEY = "site_id"
    private const val SITE_URL_KEY = "site_url"

    private var locale: Locale? = null
    private var currentAccount: AccountModel? = null
    private var currentSite: SiteModel? = null
    private var isInitialized = false

    fun initCrashLogging(context: Context) {
        if (this.isInitialized) {
            return
        }

        this.locale = localeForContext(context)

        CrashLogging.start(context, this)

        isInitialized = true
    }

    fun setCurrentAccount(account: AccountModel?) {
        this.currentAccount = account
        CrashLogging.setNeedsDataRefresh()
    }

    fun setCurrentSite(site: SiteModel?) {
        this.currentSite = site
        CrashLogging.setNeedsDataRefresh()
    }

    fun resetAccountAndSite() {
        this.currentAccount = null
        this.currentSite = null

        CrashLogging.setNeedsDataRefresh()
    }

    override fun applicationContext(): MutableMap<String, Any> {
        val siteID = this.currentSite?.siteId ?: 0
        val siteURL = this.currentSite?.url ?: ""

        return mutableMapOf(
                SITE_ID_KEY to siteID,
                SITE_URL_KEY to siteURL
        )
    }

    override fun userContext(): MutableMap<String, Any> {
        return mutableMapOf()
    }

    fun logException(tr: Throwable, tag: T? = null, message: String? = null) {
        val map = emptyMap<String, String>().toMutableMap()

        tag?.let { map[TAG_KEY] = it.name }
        message?.let { map[MESSAGE_KEY] = it }

        if (map.isEmpty()) {
            CrashLogging.log(tr)
        } else {
            CrashLogging.log(tr, map)
        }
    }

    override fun sentryDSN(): String {
        return BuildConfig.SENTRY_DSN
    }

    override fun getUserHasOptedOut(): Boolean {
        return !AppPrefs.isCrashReportingEnabled()
    }

    override fun buildType(): String {
        return BuildConfig.BUILD_TYPE
    }

    override fun releaseName(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun currentUser(): TracksUser {
        val userID = this.currentAccount?.userId.toString()
        val email = this.currentAccount?.email
        val username = this.currentAccount?.userName

        return TracksUser(userID, email, username)
    }

    override fun locale(): Locale? {
        return this.locale
    }

    private fun localeForContext(context: Context): Locale? {
        val resources = context.resources

        return if (VERSION.SDK_INT >= VERSION_CODES.N) {
            resources?.configuration?.locales?.get(0)
        } else {
            @Suppress("DEPRECATION")
            resources?.configuration?.locale
        }
    }
}
