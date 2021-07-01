package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.locale.LocaleProvider
import org.wordpress.android.fluxc.store.AccountStore
import java.util.Locale
import javax.inject.Inject

class WCCrashLoggingDataProvider @Inject constructor(
    private val localeProvider: LocaleProvider,
    private val accountStore: AccountStore,
    private val selectedSite: SelectedSite,
    private val appPrefs: AppPrefs,
    private val enqueueSendingEncryptedLogs: EnqueueSendingEncryptedLogs,
    private val uuidGenerator: UuidGenerator,
    buildConfig: BuildConfigWrapper,
) : CrashLoggingDataProvider {
    override val buildType: String = BuildConfig.BUILD_TYPE

    override val enableCrashLoggingLogs: Boolean = BuildConfig.DEBUG

    override val locale: Locale?
        get() = localeProvider.provideLocale()

    override val releaseName: String = if (buildConfig.debug) {
        DEBUG_RELEASE_NAME
    } else {
        buildConfig.versionName
    }

    override val sentryDSN: String = BuildConfig.SENTRY_DSN

    override fun applicationContextProvider(): Map<String, String> {
        return selectedSite.getIfExists()?.let {
            mapOf(
                SITE_ID_KEY to it.siteId.toString(),
                SITE_URL_KEY to it.url
            )
        }.orEmpty()
    }

    override fun crashLoggingEnabled(): Boolean {
        return appPrefs.isCrashReportingEnabled()
    }

    override fun extraKnownKeys(): List<ExtraKnownKey> {
        return listOf(EXTRA_UUID)
    }

    override fun provideExtrasForEvent(
        currentExtras: Map<ExtraKnownKey, String>,
        eventLevel: EventLevel
    ): Map<ExtraKnownKey, String> {
        return currentExtras + if (currentExtras[EXTRA_UUID] == null) {
            appendEncryptedLogsUuid(eventLevel)
        } else {
            emptyMap()
        }
    }

    private fun appendEncryptedLogsUuid(eventLevel: EventLevel): Map<ExtraKnownKey, String> {
        val uuid = uuidGenerator.generateUuid()
        enqueueSendingEncryptedLogs(
            uuid = uuid,
            eventLevel = eventLevel
        )
        return mapOf(EXTRA_UUID to uuid)
    }

    override fun shouldDropWrappingException(module: String, type: String, value: String): Boolean {
        return false
    }

    override fun userProvider(): CrashLoggingUser? {
        return accountStore.account?.let { accountModel ->
            CrashLoggingUser(
                userID = accountModel.userId.toString(),
                email = accountModel.email,
                username = accountModel.userName
            )
        }
    }

    companion object {
        const val SITE_ID_KEY = "site_id"
        const val SITE_URL_KEY = "site_url"
        const val EXTRA_UUID = "uuid"
        const val DEBUG_RELEASE_NAME = "debug"
    }
}
