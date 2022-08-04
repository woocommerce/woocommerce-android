package com.woocommerce.android.config

import com.woocommerce.android.experiment.PrologueVariant
import com.woocommerce.android.experiment.SiteLoginExperiment.SiteLoginVariant
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    fun fetchRemoteConfig()
    fun observePrologueVariant(): Flow<PrologueVariant>
    fun observeSiteLoginVariant(): Flow<SiteLoginVariant>
    fun observePerformanceMonitoringSampleRate(): Flow<Double>
}
