package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackagesState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.StoreOptionsForPackages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.withIndex
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierPackageGroups
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.StoreOptions
import javax.inject.Inject

class ObserveShippingPackages @Inject constructor(
    private val selectedSite: SelectedSite,
    private val packageRepository: WooShippingLabelPackageRepository,
    private val fetchShippingPackages: FetchShippingPackages
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<PackagesState> = packageRepository.observeShippingPackages(selectedSite.get())
        .withIndex()
        .transformLatest { (index, entity) ->
            val isFirst = index == 0
            when {
                isFirst && entity == null -> {
                    val result = fetchShippingPackages()
                    if (result.isFailure) {
                        emit(PackagesState.Error(result.exceptionOrNull()?.message ?: "Unknown error"))
                    }
                }

                entity == null -> emit(PackagesState.Error())
                else -> {
                    emit(entity.toPackagesState())
                    if (isFirst) {
                        fetchShippingPackages()
                    }
                }
            }
        }

    private fun StoreOptions.toStoreOptionsForPackages() = StoreOptionsForPackages(
        currencySymbol = currencySymbol ?: StoreOptionsForPackages.DEFAULT.currencySymbol,
        dimensionUnit = dimensionUnit ?: StoreOptionsForPackages.DEFAULT.dimensionUnit,
        weightUnit = weightUnit ?: StoreOptionsForPackages.DEFAULT.weightUnit,
        originCountry = originCountry ?: StoreOptionsForPackages.DEFAULT.originCountry
    )

    private fun WooShippingPackagesEntity.filterCarrierData(): Map<Carrier, List<CarrierPackageGroup>> =
        buildMap {
            carrierPackageGroups.parseCarrierData(WooShippingPackagesEntity.CarrierType.USPS)
                .takeIf { it.isNotEmpty() }
                ?.let { packageGroups -> put(Carrier.USPS, packageGroups) }

            carrierPackageGroups.parseCarrierData(WooShippingPackagesEntity.CarrierType.FEDEX)
                .takeIf { it.isNotEmpty() }
                ?.let { packageGroups -> put(Carrier.FEDEX, packageGroups) }

            carrierPackageGroups.parseCarrierData(WooShippingPackagesEntity.CarrierType.DHL)
                .takeIf { it.isNotEmpty() }
                ?.let { packageGroups -> put(Carrier.DHL, packageGroups) }

            carrierPackageGroups.parseCarrierData(WooShippingPackagesEntity.CarrierType.UPS)
                .takeIf { it.isNotEmpty() }
                ?.let { packageGroups -> put(Carrier.UPS, packageGroups) }
        }

    private fun List<CarrierPackageGroups>.parseCarrierData(
        carrierType: WooShippingPackagesEntity.CarrierType
    ) = find { it.carrierType == carrierType }?.let {
        it.packageGroups?.mapNotNull { group ->
            group.description?.let { description ->
                CarrierPackageGroup(
                    groupName = description,
                    packages = group.packages?.map { packageItem ->
                        PackageData.fromPackageEntity(packageItem)
                    }.orEmpty()
                )
            }
        }
    } ?: emptyList()

    private fun WooShippingPackagesEntity.toPackagesState() = PackagesState.Data(
        storeOptions = storeOptions.toStoreOptionsForPackages(),
        savedPackages = savedPackages.map { PackageData.fromPackageEntity(it) },
        carrierPackages = filterCarrierData()
    )
}
