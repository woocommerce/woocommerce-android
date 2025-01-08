package com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource

import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.DestinationAddressDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.OriginAddressDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.PackageDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.WooShippingRatesRestClient
import javax.inject.Inject

class WooShippingRatesRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val shippingRatesMapper: WooShippingRatesDatasourceMapper,
    private val restClient: WooShippingRatesRestClient
) {
    suspend fun getShippingRates(
        orderId: Long,
        selectedPackage: PackageData,
        shipTo: Address,
        shipFrom: OriginShippingAddress,
        weight: Float
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
        val packageDTO = PackageDTO(
            id = selectedPackage.id,
            boxId = "default_package",
            length = selectedPackage.length.toDouble(),
            width = selectedPackage.width.toDouble(),
            height = selectedPackage.height.toDouble(),
            weight = weight.toDouble(),
            isLetter = selectedPackage.isLetter
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
}
