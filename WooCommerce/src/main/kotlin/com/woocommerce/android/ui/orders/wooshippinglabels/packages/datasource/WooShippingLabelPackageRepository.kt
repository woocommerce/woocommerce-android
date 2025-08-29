package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CustomPackageCreationRequestData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.WooShippingLabelPackageRestClient
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.dao.WooShippingDao
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooShippingLabelPackageRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val packageMapper: WooShippingLabelPackageMapper,
    private val packageRestClient: WooShippingLabelPackageRestClient,
    private val wooShippingDao: WooShippingDao
) {
    suspend fun fetchShippingPackages(
        site: SiteModel = selectedSite.get()
    ) = packageRestClient.fetchShippingLabelPackages(site)
        .asWooResult { packageMapper(site = selectedSite.get(), response = it) }
        .also { result ->
            result.model?.takeIf { !result.isError }?.let { model -> wooShippingDao.insertShippingPackages(model) }
        }

    fun observeShippingPackages(
        site: SiteModel = selectedSite.get()
    ) = wooShippingDao.observeShippingPackages(site.localId())

    suspend fun createCustomPackage(
        requestData: List<CustomPackageCreationRequestData>,
        site: SiteModel = selectedSite.get(),
    ) = packageRestClient.postNewCustomPackage(site, requestData)
        .asWooResult { packageMapper(site = selectedSite.get(), response = it) }
        .also { result ->
            result.model?.takeIf { !result.isError }?.let { model ->
                val cachedPackages = wooShippingDao.getShippingPackages(site.localId()) ?: return@also
                wooShippingDao.insertShippingPackages(cachedPackages.copy(savedPackages = model))
            }
        }

    suspend fun saveCarrierPackage(
        savedPackageId: String,
        parentCarrierId: String,
        site: SiteModel,
    ) = packageRestClient.postPredefinedPackages(site, requestData = mapOf(parentCarrierId to listOf(savedPackageId)))
        .asWooResult { packageMapper(selectedSite.get(), response = it) }
        .also { result ->
            val cachedPackages = wooShippingDao.getShippingPackages(site.localId()) ?: return@also
            cachedPackages.markPackageSaved(parentCarrierId, savedPackageId)
                ?.let { wooShippingDao.insertShippingPackages(it) }
        }

    suspend fun deleteSavedCarrierPackage(
        packageId: String,
        isUserDefined: Boolean,
        site: SiteModel,
    ) = packageRestClient.deleteSavedCarrierPackage(site, isUserDefined, packageId)
        .asWooResult { packageMapper(site = selectedSite.get(), response = it) }
        .also {
            val cachedPackages = wooShippingDao.getShippingPackages(site.localId()) ?: return@also
            cachedPackages.markPackageRemoved(packageId, isUserDefined)
                ?.let { wooShippingDao.insertShippingPackages(it) }
        }

    private fun WooShippingPackagesEntity.markPackageSaved(
        parentCarrierId: String,
        packageId: String
    ): WooShippingPackagesEntity? {
        var targetPackage: WooShippingPackagesEntity.Package? = null

        val updatedCarrierGroups = carrierPackageGroups.map { carrierGroup ->
            if (carrierGroup.carrierType?.id == parentCarrierId) {
                carrierGroup.copy(
                    packageGroups = carrierGroup.packageGroups?.map { packageGroup ->
                        packageGroup.copy(
                            packages = packageGroup.packages?.map { p ->
                                if (p.id == packageId) {
                                    p.copy(saved = true).also { targetPackage = it }
                                } else {
                                    p
                                }
                            }
                        )
                    }
                )
            } else {
                carrierGroup
            }
        }

        targetPackage ?: return this

        val updatedSavedPackages = if (savedPackages.any { it.id == packageId }) {
            savedPackages
        } else {
            savedPackages + targetPackage
        }
        return copy(carrierPackageGroups = updatedCarrierGroups, savedPackages = updatedSavedPackages)
    }

    private fun WooShippingPackagesEntity.markPackageRemoved(
        packageId: String,
        isUserDefined: Boolean
    ): WooShippingPackagesEntity? {
        val updatedCarrierGroups = carrierPackageGroups.map { carrierGroup ->
            carrierGroup.copy(
                packageGroups = carrierGroup.packageGroups?.map { packageGroup ->
                    packageGroup.copy(
                        packages = packageGroup.packages?.map { p ->
                            if (p.id == packageId && p.isUserDefined == isUserDefined) p.copy(saved = false) else p
                        }
                    )
                }
            )
        }

        val updatedSavedPackages = savedPackages.filterNot { it.id == packageId }

        return copy(carrierPackageGroups = updatedCarrierGroups, savedPackages = updatedSavedPackages)
    }
}
