package com.woocommerce.android.config

import com.woocommerce.android.experiment.JetpackInstallationExperiment.JetpackInstallationVariant
import com.woocommerce.android.experiment.SimplifiedLoginExperiment.LoginVariant

interface RemoteConfigRepository {
    fun fetchRemoteConfig()
    fun getPerformanceMonitoringSampleRate(): Double
    fun getSimplifiedLoginVariant(): LoginVariant
    fun getJetpackInstallationVariant(): JetpackInstallationVariant
}
