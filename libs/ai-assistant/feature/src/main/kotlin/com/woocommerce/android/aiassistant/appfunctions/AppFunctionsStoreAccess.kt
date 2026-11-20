package com.woocommerce.android.aiassistant.appfunctions

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

internal data class StoreIdentity(val localSiteId: Int, val url: String)

internal fun SiteModel.identity() = StoreIdentity(localSiteId = id, url = url.orEmpty())

internal enum class StoreCheck { SAME_STORE, DIFFERENT_STORE, NO_ACCESS }

internal class AppFunctionsStoreAccess @Inject constructor(
    private val selectedSite: SelectedSite,
    private val accessPolicy: AppFunctionsAccessPolicy,
) {
    fun allowedStore(): SiteModel? = selectedSite.getIfExists()?.takeIf { accessPolicy.isAllowed() }

    fun check(store: StoreIdentity): StoreCheck {
        val current = allowedStore() ?: return StoreCheck.NO_ACCESS
        return if (current.identity() == store) StoreCheck.SAME_STORE else StoreCheck.DIFFERENT_STORE
    }
}
