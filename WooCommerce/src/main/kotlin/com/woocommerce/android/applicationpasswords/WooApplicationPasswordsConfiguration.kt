package com.woocommerce.android.applicationpasswords

import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.DeviceInfo
import jakarta.inject.Inject
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration

class WooApplicationPasswordsConfiguration @Inject constructor() : ApplicationPasswordsConfiguration {
    override val isEnabled: Boolean = true
    override val applicationName: String =
        "${BuildConfig.APPLICATION_ID}.app-client.${DeviceInfo.name.replace(' ', '-')}"
}
