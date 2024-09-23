package com.woocommerce.android.util

import com.woocommerce.android.config.WPComRemoteFeatureFlagRepository
import com.woocommerce.android.util.RemoteFeatureFlag.LOCAL_NOTIFICATION_1D_AFTER_FREE_TRIAL_EXPIRES
import com.woocommerce.android.util.RemoteFeatureFlag.LOCAL_NOTIFICATION_1D_BEFORE_FREE_TRIAL_EXPIRES
import com.woocommerce.android.util.RemoteFeatureFlag.LOCAL_NOTIFICATION_STORE_CREATION_READY
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_BLAZE
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_POS
import javax.inject.Inject

class IsRemoteFeatureFlagEnabled @Inject constructor(
    private val wpComRemoteFeatureFlagRepository: WPComRemoteFeatureFlagRepository
) {
    suspend operator fun invoke(featureFlag: RemoteFeatureFlag): Boolean {
        return when (featureFlag) {
            LOCAL_NOTIFICATION_STORE_CREATION_READY,
            LOCAL_NOTIFICATION_1D_BEFORE_FREE_TRIAL_EXPIRES,
            LOCAL_NOTIFICATION_1D_AFTER_FREE_TRIAL_EXPIRES,
            WOO_POS,
            WOO_BLAZE ->
                PackageUtils.isDebugBuild() ||
                    wpComRemoteFeatureFlagRepository.isRemoteFeatureFlagEnabled(featureFlag.remoteKey)
        }
    }
}
