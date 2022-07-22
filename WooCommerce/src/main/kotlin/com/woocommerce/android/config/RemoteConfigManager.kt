package com.woocommerce.android.config

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.woocommerce.android.R
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class RemoteConfigManager @Inject constructor(
    private val repository: RemoteConfigRepository,
    @AppCoroutineScope private val scope: CoroutineScope
) {
    companion object {
        private const val DEBUG_INTERVAL = 10L
        private const val RELEASE_INTERVAL = 31200L
    }

    private val minimumFetchIntervalInSeconds =
        if (PackageUtils.isDebugBuild())
            DEBUG_INTERVAL // 10 seconds
        else
            RELEASE_INTERVAL // 12 hours

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
