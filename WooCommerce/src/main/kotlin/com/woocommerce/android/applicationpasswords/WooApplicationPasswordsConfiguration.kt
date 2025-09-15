package com.woocommerce.android.applicationpasswords

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.DeviceInfo
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag
import jakarta.inject.Inject
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration

class WooApplicationPasswordsConfiguration @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled
) : ApplicationPasswordsConfiguration {
    override val applicationName: String =
        "${BuildConfig.APPLICATION_ID}.app-client.${DeviceInfo.name.replace(' ', '-')}"

    override suspend fun isEnabledForJetpackAccess(): Boolean {
        return appPrefs.jetpackAppPasswordsEnabled &&
            isRemoteFeatureFlagEnabled(RemoteFeatureFlag.APP_PASSWORDS_FOR_JETPACK_SITES)
    }
}
