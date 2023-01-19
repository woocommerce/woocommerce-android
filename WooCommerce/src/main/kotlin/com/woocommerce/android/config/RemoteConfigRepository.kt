package com.woocommerce.android.config

import com.woocommerce.android.experiment.RESTAPILoginExperiment.RESTAPILoginVariant
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    val fetchStatus: Flow<RemoteConfigFetchStatus>
    fun fetchRemoteConfig()
    fun getRestAPILoginVariant(): RESTAPILoginVariant
    fun getPerformanceMonitoringSampleRate(): Double
}

enum class RemoteConfigFetchStatus {
    Pending, Success, Failure
}
