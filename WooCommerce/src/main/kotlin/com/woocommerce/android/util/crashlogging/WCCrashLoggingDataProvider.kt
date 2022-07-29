package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.locale.LocaleProvider
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.AppLog

@Singleton
class WCCrashLoggingDataProvider @Inject constructor(
    private val localeProvider: LocaleProvider,
    private val accountStore: AccountStore,
    private val selectedSite: SelectedSite,
    private val appPrefs: AppPrefsWrapper,
    private val enqueueSendingEncryptedLogs: EnqueueSendingEncryptedLogs,
    private val uuidGenerator: UuidGenerator,
    private val dispatcher: Dispatcher,
    @AppCoroutineScope private val appScope: CoroutineScope,
    buildConfig: BuildConfigWrapper,
) : CrashLoggingDataProvider {

    init {
        dispatcher.register(this)
    }

    private val crashLoggingUser = MutableStateFlow(accountStore.account.toCrashLoggingUser())

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: AccountStore.OnAccountChanged) {
        appScope.launch {
            crashLoggingUser.emit(accountStore.account.toCrashLoggingUser())
        }
    }

    override val applicationContextProvider: Flow<Map<String, String>> = selectedSite
        .observe()
        .map { site ->
            AppLog.w(AppLog.T.API, "FoobarTest")
            site?.let {
                mapOf(
                    SITE_ID_KEY to site.siteId.toString(),
                    SITE_URL_KEY to site.url
                )
            }.orEmpty()
        }

    override val buildType: String = BuildConfig.BUILD_TYPE

    override val enableCrashLoggingLogs: Boolean = BuildConfig.DEBUG

    override val locale: Locale?
        get() = localeProvider.provideLocale()

    override val performanceMonitoringConfig = PerformanceMonitoringConfig.Enabled(1.0)

    override val releaseName: String = if (buildConfig.debug) {
        DEBUG_RELEASE_NAME
    } else {
        buildConfig.versionName
    }

    override val sentryDSN: String = BuildConfig.SENTRY_DSN

    override val user: Flow<CrashLoggingUser> = crashLoggingUser

    override fun crashLoggingEnabled(): Boolean {
//        return appPrefs.isCrashReportingEnabled()
        return true
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

    private fun AccountModel.toCrashLoggingUser() = CrashLoggingUser(
        userID = userId.toString(),
        email = email,
        username = userName
    )

    companion object {
        const val SITE_ID_KEY = "site_id"
        const val SITE_URL_KEY = "site_url"
        const val EXTRA_UUID = "uuid"
        const val DEBUG_RELEASE_NAME = "debug"
    }
}
