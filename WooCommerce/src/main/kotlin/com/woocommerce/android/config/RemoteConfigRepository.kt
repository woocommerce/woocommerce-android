package com.woocommerce.android.config

import com.woocommerce.android.experiment.RestAPILoginExperiment.RestAPILoginVariant
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    val fetchStatus: Flow<RemoteConfigFetchStatus>
    fun fetchRemoteConfig()
    fun getRestAPILoginVariant(): RestAPILoginVariant
    fun getPerformanceMonitoringSampleRate(): Double
}

enum class RemoteConfigFetchStatus {
    Pending, Success, Failure
}
