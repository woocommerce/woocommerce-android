package com.woocommerce.android.ui.sitepicker.sitevisibility

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType.ApplicationPasswords
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.sitepicker.SitePickerRepository
import kotlinx.coroutines.flow.first
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class GetWooVisibleSites @Inject constructor(
    private val sitePickerRepository: SitePickerRepository,
    private val visibleSitesDataStore: VisibleWooSitesDataStore,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(): List<SiteModel> =
        sitePickerRepository.getSites()
            .filter { it.hasWooCommerce && isSiteVisible(it.siteId) }
            .plusSelectedAppPasswordWooSiteIfMissing()

    private suspend fun isSiteVisible(siteId: Long): Boolean {
        return visibleSitesDataStore.isSiteVisible(siteId).first()
    }

    private fun List<SiteModel>.plusSelectedAppPasswordWooSiteIfMissing(): List<SiteModel> =
        selectedSite.getOrNull()
            ?.takeIf { selectedSite ->
                selectedSite.hasWooCommerce &&
                    selectedSite.connectionType == ApplicationPasswords &&
                    none { it.id == selectedSite.id }
            }
            ?.let { this + it }
            ?: this
}
