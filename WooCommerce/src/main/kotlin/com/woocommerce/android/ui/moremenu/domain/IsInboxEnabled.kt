package com.woocommerce.android.ui.moremenu.domain

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsInboxEnabled @Inject constructor(
    private val selectedSite: SelectedSite,

    ) {
    operator fun invoke(): Boolean {
        return FeatureFlag.MORE_MENU_INBOX.isEnabled()
    }
}
