package com.woocommerce.android.applicationpasswords

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.DeviceInfo
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import jakarta.inject.Inject
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration

class WooApplicationPasswordsConfiguration @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    private val featureFlagRepository: FeatureFlagRepository,
) : ApplicationPasswordsConfiguration {
    override val applicationName: String =
        "${BuildConfig.APPLICATION_ID}.app-client.${DeviceInfo.name.replace(' ', '-')}"

    override suspend fun isEnabledForJetpackAccess(): Boolean {
        if (!appPrefs.jetpackAppPasswordsEnabled) return false

        featureFlagRepository.awaitRemoteFlagsLoaded()
        return featureFlagRepository.isEnabled(FeatureFlag.APP_PASSWORDS_FOR_JETPACK_SITES)
    }
}
