package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.CarrierType.DHL
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.CarrierType.USPS
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CarrierPackageGroupDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CarrierPredefinedPackagesDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CustomPackageCreationResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CustomPackageDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.PackageResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.PackageStoreOptionsDTO
import javax.inject.Inject

class WooShippingLabelPackageMapper @Inject constructor() {
    operator fun invoke(response: PackageResponse): StorePackagesDAO {
        val savedPackagesResponse = response.packages?.saved?.custom ?: emptyList()
        val storeOptionsResponse = response.storeOptions ?: PackageStoreOptionsDTO()

        return StorePackagesDAO(
            storeOptions = mapStoreOptions(storeOptionsResponse),
            savedPackages = mapSavedPackages(savedPackagesResponse, response.storeOptions),
            carrierPackages = mapCarrierPackages(response.packages?.predefined, response.storeOptions)
        )
    }

    operator fun invoke(
        response: CustomPackageCreationResponse
    ): List<PackageDAO> {
        return response.custom?.map {
            PackageDAO(
                id = it.id.orEmpty(),
                name = it.name.orEmpty(),
                dimensions = it.dimensions.orEmpty(),
                weight = it.boxWeight?.toString().orEmpty(),
                isLetter = it.isLetter ?: false,
                dimensionUnit = "",
                weightUnit = ""
            )
        } ?: emptyList()
    }

    private fun mapSavedPackages(
        savedResponse: List<CustomPackageDTO>,
        storeOptions: PackageStoreOptionsDTO?
    ): List<PackageDAO> {
        return savedResponse.map {
            PackageDAO(
                id = it.id.orEmpty(),
                name = it.name.orEmpty(),
                dimensions = it.dimensions.orEmpty(),
                weight = it.boxWeight?.toString().orEmpty(),
                isLetter = it.isLetter ?: false,
                dimensionUnit = storeOptions?.dimensionUnit.orEmpty(),
                weightUnit = storeOptions?.weightUnit.orEmpty()
            )
        }
    }

    private fun mapCarrierPackages(
        carrierPackagesResponse: CarrierPredefinedPackagesDTO?,
        storeOptions: PackageStoreOptionsDTO?
    ): Map<CarrierType, CarrierDAO> {
        val uspsPackages = mutableListOf<CarrierPackageGroupDAO>().apply {
            carrierPackagesResponse?.usps?.let { usps ->
                usps.flatBoxes?.toCarrierGroup(storeOptions)?.let { add(it) }
                usps.boxes?.toCarrierGroup(storeOptions)?.let { add(it) }
                usps.expressBoxes?.toCarrierGroup(storeOptions)?.let { add(it) }
                usps.envelopes?.toCarrierGroup(storeOptions)?.let { add(it) }
            }
        }.let { CarrierDAO(it) }

        val dhlPackages = mutableListOf<CarrierPackageGroupDAO>().apply {
            carrierPackagesResponse?.dhlExpress?.let { dhl ->
                dhl.domesticAndInternationalPackages?.toCarrierGroup(storeOptions)?.let { add(it) }
            }
        }.let { CarrierDAO(it) }

        return mapOf(
            USPS to uspsPackages,
            DHL to dhlPackages
        )
    }

    private fun CarrierPackageGroupDTO.toCarrierGroup(
        storeOptions: PackageStoreOptionsDTO?
    ) = CarrierPackageGroupDAO(
        description = title.orEmpty(),
        packages = definitions?.map {
            PackageDAO(
                id = it.id.orEmpty(),
                name = it.name.orEmpty(),
                dimensions = it.outerDimensions.orEmpty(),
                weight = it.boxWeight?.toString().orEmpty(),
                isLetter = it.isLetter ?: false,
                dimensionUnit = storeOptions?.dimensionUnit.orEmpty(),
                weightUnit = storeOptions?.weightUnit.orEmpty(),
                groupName = title
            )
        } ?: emptyList()
    )

    private fun mapStoreOptions(optionsDTO: PackageStoreOptionsDTO) = StoreOptionsDAO(
        currencySymbol = optionsDTO.currencySymbol.orEmpty(),
        dimensionUnit = optionsDTO.dimensionUnit.orEmpty(),
        weightUnit = optionsDTO.weightUnit.orEmpty(),
        originCountry = optionsDTO.originCountry.orEmpty()
    )
}
