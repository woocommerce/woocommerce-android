package com.woocommerce.android.config

import androidx.annotation.VisibleForTesting
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteConfigRepository @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) : RemoteConfigRepository {
    companion object {
        private const val DEBUG_INTERVAL = 10L
        private const val RELEASE_INTERVAL = 31200L
    }

    private val minimumFetchIntervalInSeconds =
        if (PackageUtils.isDebugBuild()) {
            DEBUG_INTERVAL // 10 seconds
        } else {
            RELEASE_INTERVAL // 12 hours
        }

    private val changesTrigger = MutableSharedFlow<Unit>(replay = 1)

    private val _fetchStatus = MutableStateFlow(RemoteConfigFetchStatus.Pending)
    override val fetchStatus: Flow<RemoteConfigFetchStatus> = _fetchStatus.asStateFlow()

    private val defaultValues by lazy {
        mapOf<String, String>()
    }

    init {
        remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = this@FirebaseRemoteConfigRepository.minimumFetchIntervalInSeconds
                }
            )
            setDefaultsAsync(defaultValues)
                .addOnSuccessListener {
                    changesTrigger.tryEmit(Unit)
                }
        }
    }

    override fun fetchRemoteConfig() {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener { hasChanges ->
                WooLog.d(T.UTILS, "Remote config fetched successfully, hasChanges: $hasChanges")
                _fetchStatus.value = RemoteConfigFetchStatus.Success
                if (hasChanges) changesTrigger.tryEmit(Unit)
            }
            .addOnFailureListener {
                _fetchStatus.value = RemoteConfigFetchStatus.Failure
                WooLog.e(T.UTILS, it)
            }
    }

    @VisibleForTesting
    fun observeStringRemoteValue(key: String) = changesTrigger
        .map { remoteConfig.getString(key) }
}
