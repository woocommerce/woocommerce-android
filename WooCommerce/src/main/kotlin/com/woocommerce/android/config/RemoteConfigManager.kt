package com.woocommerce.android.config

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor(
    private val repository: RemoteConfigRepository,
    @AppCoroutineScope private val scope: CoroutineScope
) {
    private val minimumFetchIntervalInSeconds =
        if (BuildConfig.BUILD_TYPE == "debug")
            10L // 10 seconds
        else
            31200L // 12 hours

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = this@RemoteConfigManager.minimumFetchIntervalInSeconds
                }
            )
            setDefaultsAsync(R.xml.remote_config_values)
        }
    }

    fun getRemoteConfigValues() {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                val variant = remoteConfig.getString("prologue_variant")

                scope.launch {
                    repository.updatePrologueVariantValue(variant)
                }
            }
            .addOnFailureListener {
                WooLog.e(T.UTILS, it)
            }
    }
}
