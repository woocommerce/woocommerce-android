package com.woocommerce.android.config

import com.woocommerce.android.experiment.JetpackTimeoutExperiment.JetpackTimeoutPolicyVariant
import com.woocommerce.android.experiment.PrologueExperiment.PrologueVariant
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    fun fetchRemoteConfig()
    fun observePrologueVariant(): Flow<PrologueVariant>
    fun getPerformanceMonitoringSampleRate(): Double
    fun observeJetpackTimeoutPolicyVariantVariant(): Flow<JetpackTimeoutPolicyVariant>
}
