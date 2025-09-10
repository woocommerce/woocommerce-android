package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity
import javax.inject.Inject

class FetchShippingPackages @Inject constructor(
    private val packageRepository: WooShippingLabelPackageRepository,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(): Result<WooShippingPackagesEntity> {
        val response = packageRepository.fetchShippingPackages(selectedSite.get())
        val result = response.model
        return if (response.isError || result == null) {
            val message = response.error?.type?.name ?: "Unknown error"
            Result.failure(Exception(message))
        } else {
            Result.success(result)
        }
    }
}
