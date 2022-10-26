package com.woocommerce.android.config

import com.woocommerce.android.experiment.JetpackTimeoutExperiment.JetpackTimeoutPolicyVariant
import com.woocommerce.android.experiment.PrologueExperiment.PrologueVariant
import com.woocommerce.android.experiment.SimplifiedLoginExperiment.LoginVariant
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    fun fetchRemoteConfig()
    fun observePrologueVariant(): Flow<PrologueVariant>
    fun getPerformanceMonitoringSampleRate(): Double
    fun observeJetpackTimeoutPolicyVariantVariant(): Flow<JetpackTimeoutPolicyVariant>
    fun getSimplifiedLoginVariant(): LoginVariant
}
