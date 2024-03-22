package com.woocommerce.android.ui.onboarding

import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class IsLocalTaskNameYourStoreCompleted @Inject constructor(
    private val selectedSite: SelectedSite,
    private val resourceProvider: ResourceProvider
) {
    operator fun invoke(): Boolean {
        val defaultStoreName = resourceProvider.getString(R.string.store_name_default)
        return selectedSite.getIfExists()?.name != defaultStoreName
    }
}
