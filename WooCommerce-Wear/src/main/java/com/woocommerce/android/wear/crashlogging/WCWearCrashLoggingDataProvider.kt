package com.woocommerce.android.wear.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.automattic.android.tracks.crashlogging.ReleaseName
import com.woocommerce.android.wear.di.AppCoroutineScope
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.BuildConfigWrapper

@Singleton
class WCWearCrashLoggingDataProvider @Inject constructor(
    private val accountStore: AccountStore,
    @AppCoroutineScope private val appScope: CoroutineScope,
    buildConfig: BuildConfigWrapper,
    dispatcher: Dispatcher,
) : CrashLoggingDataProvider {
    override val applicationContextProvider: Flow<Map<String, String>>
        get() = TODO("Not yet implemented")
    override val buildType: String
        get() = TODO("Not yet implemented")
    override val enableCrashLoggingLogs: Boolean
        get() = TODO("Not yet implemented")
    override val locale: Locale?
        get() = TODO("Not yet implemented")
    override val performanceMonitoringConfig: PerformanceMonitoringConfig
        get() = TODO("Not yet implemented")
    override val releaseName: ReleaseName
        get() = TODO("Not yet implemented")
    override val sentryDSN: String
        get() = TODO("Not yet implemented")
    override val user: Flow<CrashLoggingUser?>
        get() = TODO("Not yet implemented")

    override fun crashLoggingEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun extraKnownKeys(): List<ExtraKnownKey> {
        TODO("Not yet implemented")
    }

    override fun provideExtrasForEvent(
        currentExtras: Map<ExtraKnownKey, String>,
        eventLevel: EventLevel
    ): Map<ExtraKnownKey, String> {
        TODO("Not yet implemented")
    }

    override fun shouldDropWrappingException(module: String, type: String, value: String): Boolean {
        TODO("Not yet implemented")
    }
}
