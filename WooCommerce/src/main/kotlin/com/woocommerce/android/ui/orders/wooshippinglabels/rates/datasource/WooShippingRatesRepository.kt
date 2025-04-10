package com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.CustomsItemDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.DestinationAddressDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.OriginAddressDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.PackageDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.PackageDTO.CommonPackageDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.PackageDTO.PackageWithCustomsDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.WooShippingRatesRestClient
import javax.inject.Inject

class WooShippingRatesRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val shippingRatesMapper: WooShippingRatesDatasourceMapper,
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
            name = "${shipFrom.firstName} ${shipFrom.lastName}",
            company = shipFrom.company,
            phone = shipFrom.phone
        )
        val destination = DestinationAddressDTO(
            address = shipTo.address1,
            city = shipTo.city,
            state = shipTo.state.codeOrRaw,
            postcode = shipTo.postcode,
            country = shipTo.country.code,
            name = "${shipTo.firstName} ${shipTo.lastName}"
        )
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
        } else {
            val rates = shippingRatesMapper(result.model)
            Result.success(rates)
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
                id = selectedPackage.id,
                boxId = "default_package",
                length = selectedPackage.length.toDouble(),
                width = selectedPackage.width.toDouble(),
                height = selectedPackage.height.toDouble(),
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
                        hsTariffNumber = it.hsTariffNumber,
                        originCountry = it.originCountryCode
                    )
                }
            )
        } else {
            CommonPackageDTO(
                id = selectedPackage.id,
                boxId = "default_package",
                length = selectedPackage.length.toDouble(),
                width = selectedPackage.width.toDouble(),
                height = selectedPackage.height.toDouble(),
                weight = weight.toDouble(),
                isLetter = selectedPackage.isLetter,
                hazmatCategory = hazmatSelection?.toHazmatCategory(),
            )
        }
    }
}
