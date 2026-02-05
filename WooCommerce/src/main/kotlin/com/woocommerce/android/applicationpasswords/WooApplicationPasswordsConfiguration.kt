package com.woocommerce.android.applicationpasswords

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.DeviceInfo
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration

class WooApplicationPasswordsConfiguration @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    private val featureFlagRepository: FeatureFlagRepository,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) : ApplicationPasswordsConfiguration {
    private val remoteFeatureFlagDeferred = appCoroutineScope.async(
        start = CoroutineStart.LAZY,
        context = Dispatchers.IO
    ) {
        featureFlagRepository.isEnabled(FeatureFlag.APP_PASSWORDS_FOR_JETPACK_SITES)
    }

    override val applicationName: String =
        "${BuildConfig.APPLICATION_ID}.app-client.${DeviceInfo.name.replace(' ', '-')}"

    override suspend fun isEnabledForJetpackAccess(): Boolean {
        return appPrefs.jetpackAppPasswordsEnabled &&
            remoteFeatureFlagDeferred.await()
    }
}
