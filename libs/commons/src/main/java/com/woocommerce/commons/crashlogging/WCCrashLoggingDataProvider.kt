package com.woocommerce.commons.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.automattic.android.tracks.crashlogging.ReleaseName
import java.util.Locale
import kotlinx.coroutines.flow.Flow

class WCCrashLoggingDataProvider: CrashLoggingDataProvider {
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
