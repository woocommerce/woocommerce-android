package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CarrierPackageGroupDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CarrierPredefinedPackagesDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.PackageCreationResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.PackageResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.PackageStoreOptionsDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.SavedPackageInfoDTO
import javax.inject.Inject

class WooShippingLabelPackageMapper @Inject constructor() {
    operator fun invoke(response: PackageResponse): StorePackagesDAO {
        val savedPackageInfoDTO = response.packages?.saved
        val storeOptionsResponse = response.storeOptions ?: PackageStoreOptionsDTO()
        val carrierPackages = mapCarrierPackages(
            response.storeOptions,
            response.packages?.predefined,
            response.packages?.saved?.predefined
        )

        return StorePackagesDAO(
            storeOptions = mapStoreOptions(storeOptionsResponse),
            savedPackages = savedPackageInfoDTO?.let {
                mapSavedPackages(it, response.storeOptions, carrierPackages)
            } ?: emptyList(),
            carrierPackages = carrierPackages
        )
    }

    operator fun invoke(
        response: PackageCreationResponse
    ): List<PackageDAO> {
        return response.custom?.map {
            PackageDAO(
                id = it.id.orEmpty(),
                name = it.name.orEmpty(),
                dimensions = it.dimensions.orEmpty(),
                weight = it.boxWeight?.toString().orEmpty(),
                isLetter = it.isLetter ?: false,
                dimensionUnit = "",
                weightUnit = "",
                saved = true
            )
        } ?: emptyList()
    }

    private fun mapSavedPackages(
        savedPackageInfoDTO: SavedPackageInfoDTO,
        storeOptions: PackageStoreOptionsDTO?,
        carrierPackages: Map<CarrierType, CarrierDAO>?
    ): List<PackageDAO> {
        val savedCarrierPackages = savedPackageInfoDTO.predefined?.flatMap { (carrierId, packageIds) ->
            val carrier = CarrierType.fromId(carrierId)
            val allPackagesForCarrier = carrierPackages?.get(carrier)
                ?.packageGroup
                ?.flatMap { it.packages }

            packageIds.mapNotNull { packageId -> allPackagesForCarrier?.find { it.id == packageId } }
        } ?: emptyList()

        val savedCustomPackages = savedPackageInfoDTO.custom?.map {
            PackageDAO(
                id = it.id.orEmpty(),
                name = it.name.orEmpty(),
                dimensions = it.dimensions.orEmpty(),
                weight = it.boxWeight?.toString().orEmpty(),
                isLetter = it.isLetter ?: false,
                dimensionUnit = storeOptions?.dimensionUnit.orEmpty(),
                weightUnit = storeOptions?.weightUnit.orEmpty(),
                saved = true
            )
        }.orEmpty()
        return savedCarrierPackages + savedCustomPackages
    }

    private fun mapCarrierPackages(
        storeOptions: PackageStoreOptionsDTO?,
        carrierPackagesResponse: CarrierPredefinedPackagesDTO?,
        savedCarrierPackageIds: Map<String, List<String>>?,
    ): Map<CarrierType, CarrierDAO> {
        val uspsPackages = buildList {
            carrierPackagesResponse?.usps?.let { usps ->
                usps.flatBoxes?.toCarrierGroup(storeOptions, savedCarrierPackageIds?.get(CarrierType.USPS.id))
                    ?.let { add(it) }
                usps.boxes?.toCarrierGroup(storeOptions, savedCarrierPackageIds?.get(CarrierType.USPS.id))
                    ?.let { add(it) }
                usps.expressBoxes?.toCarrierGroup(storeOptions, savedCarrierPackageIds?.get(CarrierType.USPS.id))
                    ?.let { add(it) }
                usps.envelopes?.toCarrierGroup(storeOptions, savedCarrierPackageIds?.get(CarrierType.USPS.id))
                    ?.let { add(it) }
            }
        }

        val dhlPackages = buildList {
            carrierPackagesResponse?.dhlExpress?.let { dhl ->
                dhl.domesticAndInternationalPackages
                    ?.toCarrierGroup(storeOptions, savedCarrierPackageIds?.get(CarrierType.DHL.id))
                    ?.let { add(it) }
            }
        }

        val upsPackages = buildList {
            carrierPackagesResponse?.ups?.let { ups ->
                ups.domesticAndInternationalPackages
                    ?.toCarrierGroup(storeOptions, savedCarrierPackageIds?.get(CarrierType.UPS.id))
                    ?.let { add(it) }
            }
        }

        return buildMap {
            if (uspsPackages.isNotEmpty()) {
                this[CarrierType.USPS] = CarrierDAO(uspsPackages)
            }
            if (dhlPackages.isNotEmpty()) {
                this[CarrierType.DHL] = CarrierDAO(dhlPackages)
            }
            if (upsPackages.isNotEmpty()) {
                this[CarrierType.UPS] = CarrierDAO(upsPackages)
            }
        }
    }

    private fun CarrierPackageGroupDTO.toCarrierGroup(
        storeOptions: PackageStoreOptionsDTO?,
        savedCarrierPackages: List<String>?
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
                groupName = title,
                saved = savedCarrierPackages?.contains(it.id.orEmpty()) ?: false
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
