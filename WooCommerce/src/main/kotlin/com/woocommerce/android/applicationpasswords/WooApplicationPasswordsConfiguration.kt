package com.woocommerce.android.applicationpasswords

import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.DeviceInfo
import com.woocommerce.android.util.FeatureFlag.APP_PASSWORDS_FOR_JETPACK_SITES
import jakarta.inject.Inject
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration

class WooApplicationPasswordsConfiguration @Inject constructor() : ApplicationPasswordsConfiguration {
    override val applicationName: String =
        "${BuildConfig.APPLICATION_ID}.app-client.${DeviceInfo.name.replace(' ', '-')}"

    override suspend fun isEnabledForJetpackAccess(): Boolean = APP_PASSWORDS_FOR_JETPACK_SITES.isEnabled()
}
