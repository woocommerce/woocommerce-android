package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingAddressDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigurationDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchasedLabelData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
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
                } ?: addressDataStore.clearOriginAddresses()
        }

    suspend fun normalizeAddress(
        site: SiteModel,
        address: Address
    ): WooResult<AddressNormalizationModel> {
        val normalizedAddress = restClient.normalizeAddress(
            site = site,
            address = mapper.toAddressDTO(address)
        )

        return if (normalizedAddress.result?.success == true) {
            normalizedAddress.asWooResult { mapper(it) }
        } else {
            WooResult(
                WooError(
                    type = WooErrorType.INVALID_RESPONSE,
                    original = GenericErrorType.INVALID_RESPONSE,
                    message = "Address normalization failed"
                )
            )
        }
    }

    suspend fun updateOriginAddress(
        site: SiteModel,
        address: Address,
        addressId: String?
    ): WooResult<OriginShippingAddress> {
        val updatedAddress = restClient.updateOriginAddress(
            site = site,
            address = mapper.toAddressDTO(address, addressId)
        )

        return if (updatedAddress.result?.success == true) {
            updatedAddress.asWooResult { mapper.toOriginAddress(it.address) }
                .also { response ->
                    response.model
                        ?.takeIf { response.isError.not() }
                        ?.let {
                            addressDataStore.updateOriginAddress(it)
                        }
                }
        } else {
            WooResult(
                WooError(
                    type = WooErrorType.INVALID_RESPONSE,
                    original = GenericErrorType.INVALID_RESPONSE,
                    message = "Address update failed"
                )
            )
        }
    }

    suspend fun updateDestinationAddress(
        site: SiteModel,
        orderId: Long,
        address: Address,
    ): WooResult<DestinationShippingAddress> {
        val updatedAddress = restClient.updateDestinationAddress(
            site = site,
            orderId = orderId,
            address = mapper.toAddressDTO(address)
        )

        return if (updatedAddress.result?.success == true) {
            updatedAddress.asWooResult {
                DestinationShippingAddress(
                    address = mapper(it.address),
                    isVerified = it.isVerified
                )
            }
        } else {
            WooResult(
                WooError(
                    type = WooErrorType.INVALID_RESPONSE,
                    original = GenericErrorType.INVALID_RESPONSE,
                    message = "Address update failed"
                )
            )
        }
    }

    suspend fun verifyDestinationAddress(
        site: SiteModel,
        orderId: Long,
    ): WooResult<DestinationShippingAddress> {
        val verifyDestinationAddress = restClient.verifyDestinationAddress(
            site = site,
            orderId = orderId,
        )

        return if (verifyDestinationAddress.result?.success == true) {
            verifyDestinationAddress.asWooResult {
                DestinationShippingAddress(
                    address = mapper(it.normalizedAddress),
                    isVerified = it.isVerified
                )
            }
        } else {
            WooResult(
                WooError(
                    type = WooErrorType.INVALID_RESPONSE,
                    original = GenericErrorType.INVALID_RESPONSE,
                    message = "Address verification failed"
                )
            )
        }
    }
}
