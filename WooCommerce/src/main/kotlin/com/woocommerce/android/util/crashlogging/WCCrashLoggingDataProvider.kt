package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.ReleaseName
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.locale.LocaleProvider
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
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCCrashLoggingDataProvider @Inject constructor(
    private val localeProvider: LocaleProvider,
    private val accountStore: AccountStore,
    private val appPrefs: AppPrefs,
    private val enqueueSendingEncryptedLogs: EnqueueSendingEncryptedLogs,
    private val uuidGenerator: UuidGenerator,
    @AppCoroutineScope private val appScope: CoroutineScope,
    specifyPerformanceMonitoringConfig: SpecifyPerformanceMonitoringConfig,
    buildConfig: BuildConfigWrapper,
    selectedSite: SelectedSite,
    dispatcher: Dispatcher,
) : CrashLoggingDataProvider {
    init {
        dispatcher.register(this)
    }

    private val crashLoggingUser = MutableStateFlow(accountStore.account?.toCrashLoggingUser())

    @Suppress("unused", "unused_parameter")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: AccountStore.OnAccountChanged) {
        appScope.launch {
            crashLoggingUser.emit(accountStore.account.toCrashLoggingUser())
        }
    }

    override val applicationContextProvider: Flow<Map<String, String>> = selectedSite
        .observe()
        .map { site ->
            site?.let {
                mapOf(
                    SITE_ID_KEY to site.siteId.takeIf { it != 0L }?.toString(),
                    SITE_URL_KEY to site.url
                )
                    .filterNotNull()
            }.orEmpty()
        }

    override val buildType: String = BuildConfig.BUILD_TYPE

    override val enableCrashLoggingLogs: Boolean = BuildConfig.DEBUG

    override val locale: Locale?
        get() = localeProvider.provideLocale()

    override val performanceMonitoringConfig = specifyPerformanceMonitoringConfig()

    override val releaseName: ReleaseName = if (buildConfig.debug) {
        ReleaseName.SetByApplication(DEBUG_RELEASE_NAME)
    } else {
        ReleaseName.SetByTracksLibrary
    }

    override val sentryDSN: String = BuildConfig.SENTRY_DSN

    override val user: Flow<CrashLoggingUser?> = crashLoggingUser

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

    private fun AccountModel.toCrashLoggingUser(): CrashLoggingUser? {
        if (userId == 0L) return null

        return CrashLoggingUser(
            userID = userId.toString(),
            email = email,
            username = userName
        )
    }

    companion object {
        const val SITE_ID_KEY = "site_id"
        const val SITE_URL_KEY = "site_url"
        const val EXTRA_UUID = "uuid"
        const val DEBUG_RELEASE_NAME = "debug"
    }
}
