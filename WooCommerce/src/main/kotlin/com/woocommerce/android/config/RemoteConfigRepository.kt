package com.woocommerce.android.config

import com.woocommerce.android.experiment.JetpackTimeoutExperiment.JetpackTimeoutPolicyVariant
import com.woocommerce.android.experiment.LoginButtonSwapExperiment.LoginButtonSwapVariant
import com.woocommerce.android.experiment.MagicLinkRequestExperiment.MagicLinkRequestVariant
import com.woocommerce.android.experiment.MagicLinkSentScreenExperiment.MagicLinkSentScreenVariant
import com.woocommerce.android.experiment.PrologueExperiment.PrologueVariant
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    fun fetchRemoteConfig()
    fun observePrologueVariant(): Flow<PrologueVariant>
    fun observeMagicLinkSentScreenVariant(): Flow<MagicLinkSentScreenVariant>
    fun observeMagicLinkRequestVariant(): Flow<MagicLinkRequestVariant>
    fun observeLoginButtonsSwapVariant(): Flow<LoginButtonSwapVariant>
    fun getPerformanceMonitoringSampleRate(): Double
    fun observeJetpackTimeoutPolicyVariantVariant(): Flow<JetpackTimeoutPolicyVariant>
}
