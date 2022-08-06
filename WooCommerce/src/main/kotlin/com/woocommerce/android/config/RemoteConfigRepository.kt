package com.woocommerce.android.config

import com.woocommerce.android.experiment.AutomaticMagicLinkRequestExperiment.AutomaticMagicLinkRequestVariant
import com.woocommerce.android.experiment.MagicLinkSentScreenExperiment.MagicLinkSentScreenVariant
import com.woocommerce.android.experiment.PrologueExperiment.PrologueVariant
import com.woocommerce.android.experiment.SiteLoginExperiment.SiteLoginVariant
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    fun fetchRemoteConfig()
    fun observePrologueVariant(): Flow<PrologueVariant>
    fun observeSiteLoginVariant(): Flow<SiteLoginVariant>
    fun observeMagicLinkSentScreenVariant(): Flow<MagicLinkSentScreenVariant>
    fun observeAutomaticMagicLinkRequestVariant(): Flow<AutomaticMagicLinkRequestVariant>
    fun getPerformanceMonitoringSampleRate(): Double
}
