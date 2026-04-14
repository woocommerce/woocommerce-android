package com.woocommerce.android.ui.dashboard.domain

import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class GetAnalyticsDateTypeInfo @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
) {
    data class AnalyticsDateTypeInfo(
        val dateTypeLabel: String,
        val analyticsSettingsUrl: String,
    )

    suspend operator fun invoke(): Result<AnalyticsDateTypeInfo> {
        val site = selectedSite.get()
        val result = wooCommerceStore.fetchDateTypeSetting(site)
        val dateTypeValue = result.model ?: "date_paid"
        val label = mapDateTypeToLabel(dateTypeValue)
        val url = "${site.adminUrlOrDefault}admin.php?page=wc-admin&path=%2Fanalytics%2Fsettings"
        return Result.success(AnalyticsDateTypeInfo(dateTypeLabel = label, analyticsSettingsUrl = url))
    }

    private fun mapDateTypeToLabel(value: String): String = when (value) {
        "date_created" -> "date created"
        "date_completed" -> "date completed"
        else -> "date paid"
    }
}
