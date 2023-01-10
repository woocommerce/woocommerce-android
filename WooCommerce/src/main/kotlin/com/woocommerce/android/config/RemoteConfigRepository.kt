package com.woocommerce.android.config

import com.woocommerce.android.experiment.JetpackInstallationExperiment.JetpackInstallationVariant

interface RemoteConfigRepository {
    fun fetchRemoteConfig()
    fun getPerformanceMonitoringSampleRate(): Double
    fun getJetpackInstallationVariant(): JetpackInstallationVariant
}
