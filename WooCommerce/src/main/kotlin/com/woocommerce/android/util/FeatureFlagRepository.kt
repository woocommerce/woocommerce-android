package com.woocommerce.android.util

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.config.WPComRemoteFeatureFlagRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagRepository @Inject constructor(
    private val remoteFeatureFlagRepository: WPComRemoteFeatureFlagRepository
) {
    suspend fun isEnabled(flag: FeatureFlag) = getFlagState(flag).effectiveValue

    private fun getOverrideValue(flag: FeatureFlag): Boolean? = try {
        AppPrefs.getFeatureFlagOverride(flag)
    } catch (_: UninitializedPropertyAccessException) {
        null
    }

    private suspend fun getRemoteValue(key: String): Boolean? =
        remoteFeatureFlagRepository.isRemoteFeatureFlagEnabled(key)

    fun setFlagOverride(flag: FeatureFlag, enabled: Boolean) {
        AppPrefs.setFeatureFlagOverride(flag, enabled)
    }

    fun removeFlagOverride(flag: FeatureFlag) {
        AppPrefs.removeFeatureFlagOverride(flag)
    }

    suspend fun getFlagState(flag: FeatureFlag) = FeatureFlagState(
        flag = flag,
        defaultValue = flag.default,
        remoteValue = getRemoteValue(flag.key),
        overrideValue = getOverrideValue(flag)
    )

    data class FeatureFlagState(
        val flag: FeatureFlag,
        val defaultValue: Boolean,
        val remoteValue: Boolean?,
        val overrideValue: Boolean?
    ) {
        val effectiveValue: Boolean
            get() = overrideValue ?: remoteValue ?: defaultValue
    }
}
