package com.woocommerce.android.config

import com.woocommerce.android.experiment.RestAPILoginExperiment.RestAPILoginVariant

interface RemoteConfigRepository {
    fun fetchRemoteConfig()
    fun getRestAPILoginVariant(): RestAPILoginVariant
    fun getPerformanceMonitoringSampleRate(): Double
}
