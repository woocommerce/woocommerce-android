package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CarrierPackageGroupDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CarrierPredefinedPackagesDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.PackageCreationResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.PackageResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.PackageStoreOptionsDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.SavedPackageInfoDTO
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierPackageGroup
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierPackageGroups
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierType
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.Package
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.StoreOptions
import javax.inject.Inject

class WooShippingLabelPackageMapper @Inject constructor() {
    operator fun invoke(site: SiteModel, response: PackageResponse): WooShippingPackagesEntity {
        val storeOptionsResponse = response.storeOptions ?: PackageStoreOptionsDTO()
        val savedPackageInfoDTO = response.packages?.saved
        val carrierPackageGroups = mapCarrierPackages(
            storeOptions = response.storeOptions,
            carrierPackagesResponse = response.packages?.predefined,
            savedCarrierPackageIds = response.packages?.saved?.predefined
        )

        return WooShippingPackagesEntity(
            localSiteId = site.localId(),
            storeOptions = mapStoreOptions(storeOptionsResponse),
            savedPackages = savedPackageInfoDTO?.let {
                mapSavedPackages(
                    savedPackageInfoDTO = it,
                    storeOptions = response.storeOptions,
                    carrierPackageGroups = carrierPackageGroups
                )
            } ?: emptyList(),
            carrierPackageGroups = carrierPackageGroups
        )
    }

    operator fun invoke(site: SiteModel, response: PackageCreationResponse): List<Package> = response.custom?.map {
        Package(
            id = it.id.orEmpty(),
            name = it.name.orEmpty(),
            dimensions = it.dimensions.orEmpty(),
            weight = it.boxWeight?.toString().orEmpty(),
            isLetter = it.isLetter ?: false,
            isUserDefined = it.isUserDefined == true,
            dimensionUnit = "",
            weightUnit = "",
            saved = true
        )
    } ?: emptyList()

    private fun mapSavedPackages(
        savedPackageInfoDTO: SavedPackageInfoDTO,
        storeOptions: PackageStoreOptionsDTO?,
        carrierPackageGroups: List<CarrierPackageGroups>?
    ): List<Package> {
        val savedCarrierPackages = savedPackageInfoDTO.predefined?.flatMap { (carrierId, packageIds) ->
            val carrier = CarrierType.fromId(carrierId)
            val allPackagesForCarrier = carrierPackageGroups?.find { it.carrierType == carrier }?.packageGroups
                ?.flatMap { it.packages.orEmpty() }

            packageIds.mapNotNull { packageId -> allPackagesForCarrier?.find { it.id == packageId } }
        } ?: emptyList()

        val savedCustomPackages = savedPackageInfoDTO.custom?.map {
            Package(
                id = it.id.orEmpty(),
                name = it.name.orEmpty(),
                dimensions = it.dimensions.orEmpty(),
                weight = it.boxWeight?.toString().orEmpty(),
                isLetter = it.isLetter ?: false,
                isUserDefined = it.isUserDefined == true,
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
    ): List<CarrierPackageGroups> {
        fun groupsFor(
            type: CarrierType,
            groups: List<CarrierPackageGroupDTO?>
        ): CarrierPackageGroups? {
            val savedIds = savedCarrierPackageIds?.get(type.id)
            val mapped = groups
                .filterNotNull()
                .map { it.toCarrierGroup(storeOptions, savedIds) }
            return mapped.takeIf { it.isNotEmpty() }?.let {
                CarrierPackageGroups(type, it)
            }
        }

        val usps = carrierPackagesResponse?.usps
        val fedex = carrierPackagesResponse?.fedex
        val dhl = carrierPackagesResponse?.dhlExpress
        val ups = carrierPackagesResponse?.ups

        return listOfNotNull(
            usps?.let {
                groupsFor(
                    CarrierType.USPS,
                    listOf(it.flatBoxes, it.boxes, it.expressBoxes, it.envelopes)
                )
            },
            fedex?.let {
                groupsFor(
                    CarrierType.FEDEX,
                    listOf(it.express, it.international)
                )
            },
            dhl?.let {
                groupsFor(
                    CarrierType.DHL,
                    listOf(it.domesticAndInternationalPackages)
                )
            },
            ups?.let {
                groupsFor(
                    CarrierType.UPS,
                    listOf(it.domesticAndInternationalPackages)
                )
            }
        )
    }

    private fun CarrierPackageGroupDTO.toCarrierGroup(
        storeOptions: PackageStoreOptionsDTO?,
        savedCarrierPackages: List<String>?
    ) = CarrierPackageGroup(
        description = title.orEmpty(),
        packages = definitions?.map {
            Package(
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

    private fun mapStoreOptions(optionsDTO: PackageStoreOptionsDTO) = StoreOptions(
        currencySymbol = optionsDTO.currencySymbol.orEmpty(),
        dimensionUnit = optionsDTO.dimensionUnit.orEmpty(),
        weightUnit = optionsDTO.weightUnit.orEmpty(),
        originCountry = optionsDTO.originCountry.orEmpty()
    )
}
