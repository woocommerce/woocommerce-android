package com.woocommerce.android.config

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.woocommerce.android.R
import com.woocommerce.android.experiment.PrologueVariant
import com.woocommerce.android.experiment.SiteLoginExperiment.SiteLoginVariant
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteConfigRepository @Inject constructor() : RemoteConfigRepository {
    companion object {
        const val PROLOGUE_VARIANT_KEY = "prologue_variant"
        const val SITE_CREDENTIALS_EXPERIMENT_VARIANT_KEY = "site_credentials_emphasis"
        private const val DEBUG_INTERVAL = 10L
        private const val RELEASE_INTERVAL = 31200L
    }

    private val minimumFetchIntervalInSeconds =
        if (PackageUtils.isDebugBuild())
            DEBUG_INTERVAL // 10 seconds
        else
            RELEASE_INTERVAL // 12 hours

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
    private val changesTrigger = MutableSharedFlow<Unit>(replay = 1)

    init {
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = this@FirebaseRemoteConfigRepository.minimumFetchIntervalInSeconds
                }
            )
            setDefaultsAsync(R.xml.remote_config_values)
        }
        changesTrigger.tryEmit(Unit)
    }

    override fun fetchRemoteConfig() {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener { hasChanges ->
                if (hasChanges) changesTrigger.tryEmit(Unit)
            }
            .addOnFailureListener {
                WooLog.e(T.UTILS, it)
            }
    }

    override fun observePrologueVariant(): Flow<PrologueVariant> =
        observeStringRemoteValue(PROLOGUE_VARIANT_KEY)
            .map { PrologueVariant.valueOf(it.uppercase()) }

    override fun observeSiteLoginVariant(): Flow<SiteLoginVariant> =
        observeStringRemoteValue(SITE_CREDENTIALS_EXPERIMENT_VARIANT_KEY)
            .map { SiteLoginVariant.valueOf(it.uppercase()) }

    private fun observeStringRemoteValue(key: String) = changesTrigger
        .map { remoteConfig.getString(key) }
}
