package com.woocommerce.android.util

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.di.AppCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.mobile.FeatureFlagsStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagRepository @Inject constructor(
    featureFlagsStore: FeatureFlagsStore,
    @AppCoroutineScope appCoroutineScope: CoroutineScope,
) {
    private val remoteFlagValues = MutableStateFlow<Map<String, Boolean>?>(null)

    init {
        appCoroutineScope.launch {
            featureFlagsStore.observeFeatureFlags().collect { remoteFlags ->
                remoteFlagValues.value = remoteFlags.associate { remoteFlag -> remoteFlag.key to remoteFlag.value }
            }
        }
    }

    fun isEnabled(flag: FeatureFlag) = getFlagState(flag).effectiveValue

    suspend fun awaitRemoteFlagsLoaded() {
        remoteFlagValues.filter { it != null }.first()
    }

    private fun getOverrideValue(flag: FeatureFlag): Boolean? = try {
        AppPrefs.getFeatureFlagOverride(flag)
    } catch (_: UninitializedPropertyAccessException) {
        null
    }

    fun setFlagOverride(flag: FeatureFlag, enabled: Boolean) {
        AppPrefs.setFeatureFlagOverride(flag, enabled)
    }

    fun removeFlagOverride(flag: FeatureFlag) {
        AppPrefs.removeFeatureFlagOverride(flag)
    }

    fun getFlagState(flag: FeatureFlag) = FeatureFlagState(
        flag = flag,
        localValue = flag.localValue,
        remoteValue = remoteFlagValues.value?.get(flag.remoteFlagKey),
        overrideValue = getOverrideValue(flag)
    )

    data class FeatureFlagState(
        val flag: FeatureFlag,
        val localValue: Boolean,
        val remoteValue: Boolean?,
        val overrideValue: Boolean?
    ) {
        val effectiveValue: Boolean
            get() = overrideValue ?: (localValue && (remoteValue ?: true))
    }
}
