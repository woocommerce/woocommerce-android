package com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.CustomsItemDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingNetworkingMapper
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain.InvalidDestinationNameRateException
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.OriginAddressDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.PackageDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.PackageDTO.CommonPackageDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.PackageDTO.PackageWithCustomsDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.WooShippingRatesDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.WooShippingRatesRestClient
import com.woocommerce.android.util.StringUtils.combineStrings
import javax.inject.Inject

class WooShippingRatesRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val shippingRatesMapper: WooShippingRatesDatasourceMapper,
    private val shippingNetworkingMapper: WooShippingNetworkingMapper,
    private val restClient: WooShippingRatesRestClient
) {
    @Suppress("LongParameterList")
    suspend fun getShippingRates(
        orderId: Long,
        selectedPackage: PackageData,
        shipTo: Address,
        shipFrom: OriginShippingAddress,
        weight: Float,
        customsData: CustomsData?,
        hazmatSelection: ShippingLabelHazmatCategory? = null
    ): Result<List<WooShippingRateOptionsModel>> {
        val origin = OriginAddressDTO(
            address = shipFrom.address1,
            address2 = shipFrom.address2,
            city = shipFrom.city,
            state = shipFrom.state,
            postcode = shipFrom.postcode,
            country = shipFrom.country,
            name = combineStrings(shipFrom.firstName.orEmpty(), shipFrom.lastName.orEmpty())
                .takeIf { it.isNotEmpty() },
            company = shipFrom.company,
            phone = shipFrom.phone
        )
        val destination = shippingNetworkingMapper.toDestinationAddressDTO(shipTo)
        val packageDTO = createPackageDTO(
            selectedPackage = selectedPackage,
            weight = weight,
            customsData = customsData,
            hazmatSelection = hazmatSelection
        )

        val result = restClient.getShippingRates(
            site = selectedSite.get(),
            orderId = orderId.toString(),
            origin = origin,
            destination = destination,
            packages = listOf(packageDTO)
        )

        return if (result.isError) {
            Result.failure(Exception(result.error.message))
        } else if (hasInvalidDestinationNameRateError(result.model)) {
            Result.failure(InvalidDestinationNameRateException())
        } else {
            Result.success(shippingRatesMapper(result.model))
        }
    }

    private fun hasInvalidDestinationNameRateError(
        ratesResponse: Map<String, Map<String, WooShippingRatesDTO>>?
    ): Boolean {
        val defaultRates = ratesResponse?.get(DEFAULT_PACKAGE_ID)?.get(DEFAULT_RATE_OPTION_ID) ?: return false

        return defaultRates.errors.any { error ->
            error.code == RATE_ERROR_CODE &&
                error.message?.contains(INVALID_DESTINATION_NAME_ERROR, ignoreCase = true) == true
        }
    }

    private fun createPackageDTO(
        selectedPackage: PackageData,
        weight: Float,
        customsData: CustomsData?,
        hazmatSelection: ShippingLabelHazmatCategory?
    ): PackageDTO {
        return if (customsData != null) {
            PackageWithCustomsDTO(
                id = "default_package",
                boxId = selectedPackage.id,
                length = selectedPackage.length.toDouble(),
                width = selectedPackage.width.toDouble(),
                height = selectedPackage.safeHeight,
                weight = weight.toDouble(),
                isLetter = selectedPackage.isLetter,
                contentsType = customsData.contentType.name.toLowerCase(Locale.current),
                contentExplanation = customsData.contentDescription,
                restrictionType = customsData.restrictionType.name.toLowerCase(Locale.current),
                restrictionComments = customsData.restrictionDescription,
                isReturnToSender = if (customsData.isReturnToSender) "return" else "abandon",
                itn = customsData.itn,
                hazmatCategory = hazmatSelection?.toHazmatCategory(),
                items = customsData.items.map {
                    CustomsItemDTO(
                        productId = it.productID,
                        description = it.description,
                        quantity = it.quantity,
                        value = it.value.toDouble(),
                        weight = it.weight.toDouble(),
                        hsTariffNumber = it.hsTariffNumber.replace(Regex("""\D"""), ""),
                        originCountry = it.originCountryCode
                    )
                }
            )
        } else {
            CommonPackageDTO(
                id = "default_package",
                boxId = selectedPackage.id,
                length = selectedPackage.length.toDouble(),
                width = selectedPackage.width.toDouble(),
                height = selectedPackage.safeHeight,
                weight = weight.toDouble(),
                isLetter = selectedPackage.isLetter,
                hazmatCategory = hazmatSelection?.toHazmatCategory(),
            )
        }
    }

    private companion object {
        const val DEFAULT_PACKAGE_ID = "default_package"
        const val DEFAULT_RATE_OPTION_ID = "default"
        const val RATE_ERROR_CODE = "rate_error"
        const val INVALID_DESTINATION_NAME_ERROR = "shipment.to_address: invalid name"
    }
}
