package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class WooPosLocalCatalogFileApproachEnabled @Inject constructor() {
    operator fun invoke(): Boolean = FeatureFlag.WOO_POS_LOCAL_CATALOG_FILE_APPROACH.isEnabled()
}
