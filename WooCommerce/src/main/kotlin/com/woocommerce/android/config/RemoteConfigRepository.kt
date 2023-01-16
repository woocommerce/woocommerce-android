package com.woocommerce.android.config

interface RemoteConfigRepository {
    fun fetchRemoteConfig()
    fun getPerformanceMonitoringSampleRate(): Double
}
