package com.woocommerce.android.config

import androidx.annotation.VisibleForTesting
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.woocommerce.android.experiment.JetpackTimeoutExperiment.JetpackTimeoutPolicyVariant
import com.woocommerce.android.experiment.PrologueExperiment.PrologueVariant
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
        private const val PERFORMANCE_MONITORING_SAMPLE_RATE_KEY = "wc_android_performance_monitoring_sample_rate"
        private const val JETPACK_TIMEOUT_POLICY_VARIANT_KEY = "jetpack_timeout_policy_variant"
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
            JETPACK_TIMEOUT_POLICY_VARIANT_KEY to JetpackTimeoutPolicyVariant.CONTROL.name
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
                .addOnSuccessListener {
                    changesTrigger.tryEmit(Unit)
                }
        }
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

    override fun getPerformanceMonitoringSampleRate(): Double =
        remoteConfig.getDouble(PERFORMANCE_MONITORING_SAMPLE_RATE_KEY)

    override fun observeJetpackTimeoutPolicyVariantVariant(): Flow<JetpackTimeoutPolicyVariant> =
        observeStringRemoteValue(JETPACK_TIMEOUT_POLICY_VARIANT_KEY)
            .map { JetpackTimeoutPolicyVariant.valueOf(it.uppercase()) }
            .catch {
                crashLogging.get().recordException(it)
                emit(JetpackTimeoutPolicyVariant.valueOf(defaultValues[JETPACK_TIMEOUT_POLICY_VARIANT_KEY]!!))
            }

    private fun observeStringRemoteValue(key: String) = changesTrigger
        .map { remoteConfig.getString(key) }
}
