package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingAddressDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigurationDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchasedLabelData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import javax.inject.Inject

class WooShippingLabelRepository @Inject constructor(
    private val restClient: WooShippingLabelRestClient,
    private val mapper: WooShippingNetworkingMapper,
    private val configurationDataStore: WooShippingConfigurationDataStore,
    private val addressDataStore: WooShippingAddressDataStore
) {
    suspend fun fetchShippingLabelPrinting(
        site: SiteModel,
        labelIds: List<Long>,
        paperSize: String
    ) = restClient.fetchShippingLabelPrinting(
        site = site,
        labelIds = labelIds,
        paperSize = paperSize
    ).asWooResult()

    suspend fun fetchAccountSettings(
        site: SiteModel,
    ) = restClient.fetchAccountSettings(
        site = site,
    ).asWooResult { mapper(it.storeOptions) }
        .also { response ->
            response.model
                ?.takeIf { response.isError.not() }
                ?.let {
                    configurationDataStore.saveStoreOptions(it)
                }
        }

    suspend fun fetchPurchasedShippingLabels(
        site: SiteModel,
        orderId: Long,
    ) = restClient.fetchPurchasedShippingLabels(
        site = site,
        orderId = orderId,
    ).asWooResult { it.shippingLabels?.map { label -> mapper(label) } }

    suspend fun fetchShippingLabelStatus(
        site: SiteModel,
        orderId: Long,
        labelId: Long,
    ) = restClient.fetchShippingLabelStatus(
        site = site,
        orderId = orderId,
        labelId = labelId,
    ).asWooResult { response ->
        response.shippingLabel?.let {
            mapper(it).status
        } ?: ShippingLabelStatus.Unknown
    }

    @Suppress("LongParameterList")
    suspend fun purchaseShippingLabel(
        site: SiteModel,
        orderId: Long,
        shippableItems: List<Long>,
        selectedPackage: PackageData,
        shipTo: Address,
        shipFrom: OriginShippingAddress,
        selectedRate: WooShippingRateModel,
        weight: Float,
        lastOrderComplete: Boolean,
    ): WooResult<PurchasedLabelData> {
        val origin = mapper.toOriginAddressPurchaseDTO(shipFrom)
        val destination = mapper.toDestinationAddressDTO(shipTo)
        val packageDTO = mapper.toPackagePurchaseDTO(
            selectedPackage = selectedPackage,
            selectedRate = selectedRate,
            shippableItems = shippableItems,
            weight = weight
        )
        val rateDTO = mapper.toRateDTO(selectedRate)
        return restClient.purchaseShippingLabel(
            site = site,
            orderId = orderId,
            origin = origin,
            destination = destination,
            selectedPackage = packageDTO,
            selectedRate = rateDTO,
            markOrderComplete = lastOrderComplete
        ).asWooResult { mapper(it) }
    }

    suspend fun fetchOriginAddresses(
        site: SiteModel
    ) = restClient.fetchOriginAddresses(site = site)
        .asWooResult { mapper(it) }
        .also { response ->
            response.model
                ?.takeIf { response.isError.not() }
                ?.let {
                    addressDataStore.saveOriginAddresses(it)
                }
        }
}
