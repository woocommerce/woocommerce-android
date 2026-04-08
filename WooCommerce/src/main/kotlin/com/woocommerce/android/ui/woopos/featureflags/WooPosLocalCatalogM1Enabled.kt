package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject

class WooPosLocalCatalogM1Enabled @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository
) {
    operator fun invoke(): Boolean =
        featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_LOCAL_CATALOG_M1) && AppPrefs.wooPosLocalCatalogEnabled
}
