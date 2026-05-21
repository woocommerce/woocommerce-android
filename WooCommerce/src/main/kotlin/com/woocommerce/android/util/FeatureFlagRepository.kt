package com.woocommerce.android.util

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.di.AppCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    fun observeIsEnabled(flag: FeatureFlag): Flow<Boolean> {
        return remoteFlagValues
            .map { remoteValues -> getFlagState(flag, remoteValues).effectiveValue }
            .distinctUntilChanged()
    }

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

    fun getFlagState(flag: FeatureFlag) = getFlagState(flag, remoteFlagValues.value)

    private fun getFlagState(flag: FeatureFlag, remoteValues: Map<String, Boolean>?) = FeatureFlagState(
        flag = flag,
        localValue = flag.localValue,
        remoteValue = remoteValues?.get(flag.remoteFlagKey),
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
