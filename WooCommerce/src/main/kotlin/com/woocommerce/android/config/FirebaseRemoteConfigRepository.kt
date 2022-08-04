package com.woocommerce.android.config

import androidx.annotation.VisibleForTesting
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.woocommerce.android.experiment.PrologueVariant
import com.woocommerce.android.experiment.SiteLoginExperiment.SiteLoginVariant
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class FirebaseRemoteConfigRepository @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val crashLogging: Provider<CrashLogging>
) : RemoteConfigRepository {
    companion object {
        @VisibleForTesting
        const val PROLOGUE_VARIANT_KEY = "prologue_variant"
        private const val SITE_CREDENTIALS_EXPERIMENT_VARIANT_KEY = "site_credentials_emphasis"
        private const val PERFORMANCE_MONITORING_SAMPLE_RATE_KEY = "wc_android_performance_monitoring_sample_rate"
        private const val DEBUG_INTERVAL = 10L
        private const val RELEASE_INTERVAL = 31200L
    }

    private val minimumFetchIntervalInSeconds =
        if (PackageUtils.isDebugBuild())
            DEBUG_INTERVAL // 10 seconds
        else
            RELEASE_INTERVAL // 12 hours

    private val changesTrigger = MutableSharedFlow<Unit>(replay = 1)

    private val defaultValues by lazy {
        mapOf(
            PROLOGUE_VARIANT_KEY to PrologueVariant.CONTROL.name,
            SITE_CREDENTIALS_EXPERIMENT_VARIANT_KEY to SiteLoginVariant.EMAIL_LOGIN.name,
        )
    }

    init {
        remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = this@FirebaseRemoteConfigRepository.minimumFetchIntervalInSeconds
                }
            )
            setDefaultsAsync(defaultValues)
        }
        changesTrigger.tryEmit(Unit)
    }

    override fun fetchRemoteConfig() {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener { hasChanges ->
                WooLog.d(T.UTILS, "Remote config fetched successfully, hasChanges: $hasChanges")
                if (hasChanges) changesTrigger.tryEmit(Unit)
            }
            .addOnFailureListener {
                WooLog.e(T.UTILS, it)
            }
    }

    override fun observePrologueVariant(): Flow<PrologueVariant> =
        observeStringRemoteValue(PROLOGUE_VARIANT_KEY)
            .map { PrologueVariant.valueOf(it.uppercase()) }
            .catch {
                crashLogging.get().recordException(it)
                emit(PrologueVariant.valueOf(defaultValues[PROLOGUE_VARIANT_KEY]!!))
            }

    override fun observeSiteLoginVariant(): Flow<SiteLoginVariant> =
        observeStringRemoteValue(SITE_CREDENTIALS_EXPERIMENT_VARIANT_KEY)
            .map { SiteLoginVariant.valueOf(it.uppercase()) }
            .catch {
                crashLogging.get().recordException(it)
                emit(SiteLoginVariant.valueOf(defaultValues[SITE_CREDENTIALS_EXPERIMENT_VARIANT_KEY]!!))
            }

    override fun observePerformanceMonitoringSampleRate(): Flow<Double> =
        observeDoubleRemoteValue(PERFORMANCE_MONITORING_SAMPLE_RATE_KEY)
            .catch {
                crashLogging.get().recordException(it)
                emit(0.0)
            }

    private fun observeStringRemoteValue(key: String) = changesTrigger
        .map { remoteConfig.getString(key) }

    private fun observeDoubleRemoteValue(key: String) = changesTrigger
        .map { remoteConfig.getDouble(key) }
}
