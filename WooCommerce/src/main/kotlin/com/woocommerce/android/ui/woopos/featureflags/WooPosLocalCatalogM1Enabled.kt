package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.REMOTE_WOO_POS_LOCAL_CATALOG_M1
import javax.inject.Inject

@Suppress("unused")
class WooPosLocalCatalogM1Enabled @Inject constructor(
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled
) {
    suspend operator fun invoke(): Boolean {
        return isRemoteFeatureFlagEnabled(REMOTE_WOO_POS_LOCAL_CATALOG_M1)
    }
}
