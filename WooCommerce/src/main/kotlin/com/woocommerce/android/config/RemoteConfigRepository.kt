package com.woocommerce.android.config

import com.woocommerce.android.experiment.SimplifiedLoginExperiment.LoginVariant
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    fun fetchRemoteConfig()
    fun getPerformanceMonitoringSampleRate(): Double
    fun getSimplifiedLoginVariant(): LoginVariant
}
