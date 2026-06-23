package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.ShippingLabelHazmatCategory
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingAccountSettingsDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingAddressDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingEligibilityDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchasedLabelData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingSelectedRateModel
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.dao.WooShippingDao
import javax.inject.Inject

class WooShippingLabelRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val restClient: WooShippingLabelRestClient,
    private val mapper: WooShippingNetworkingMapper,
    private val eligibilityDataStore: WooShippingEligibilityDataStore,
    private val wooShippingDao: WooShippingDao,
    private val accountSettingsDataStore: WooShippingAccountSettingsDataStore,
    private val addressDataStore: WooShippingAddressDataStore
) {
    suspend fun fetchShippingEligibility(
        site: SiteModel,
        orderId: Long,
    ) = restClient.fetchShippingEligibility(site = site, orderId = orderId).asWooResult().also { response ->
        response.model
            ?.takeIf { response.isError.not() }
            ?.let { eligibilityDataStore.saveEligibility(orderId, it.isEligible) }
    }

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
    ).asWooResult { mapper(it) }
        .also { response ->
            response.model
                ?.takeIf { response.isError.not() }
                ?.let {
                    accountSettingsDataStore.saveAccountSettings(it)
                }
        }

    suspend fun updateAccountSettings(
        site: SiteModel,
        selectedPaymentMethodId: Long?,
        emailReceipts: Boolean,
    ): WooResult<Unit> {
        return restClient.updateAccountSettings(site, selectedPaymentMethodId, emailReceipts).asWooResult()
            .also { result ->
                if (!result.isError) {
                    accountSettingsDataStore.updateAccountSettings {
                        it.copy(
                            paymentMethodOptions = it.paymentMethodOptions.copy(
                                selectedPaymentId = selectedPaymentMethodId,
                                emailReceipts = emailReceipts
                            )
                        )
                    }
                }
            }
    }

    suspend fun fetchConfig(site: SiteModel, orderId: Long): WooResult<ConfigResponse> =
        restClient.fetchConfig(site, orderId)
            .asWooResult()
            .also { response ->
                response.model
                    ?.takeIf { !response.isError }
                    ?.let { model ->
                        wooShippingDao.replaceShipments(
                            mapper(model.config.shipments ?: emptyMap(), site, orderId)
                        )
                        wooShippingDao.replaceLabels(
                            mapper(model.config.shippingLabelData, site, orderId)
                        )
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
    ).also {
        it.result?.shippingLabel?.let { labelDTO ->
            // The API doesn't return the hazmat category, origin and destination addresses,
            // we get it from the already cached labels in the database after the purchase request
            val currentLabel = wooShippingDao.getLabel(
                selectedSite.get().localId(),
                LocalOrRemoteId.RemoteId(orderId),
                labelId
            )
            val updatedLabel = mapper(labelDTO, site, orderId).copy(
                hazmatCategory = currentLabel?.hazmatCategory,
                originAddress = currentLabel?.originAddress,
                destinationAddress = currentLabel?.destinationAddress
            )
            wooShippingDao.insertLabel(updatedLabel)
        }
    }.asWooResult { response ->
        response.shippingLabel?.let { mapper(it) }
    }

    @Suppress("LongParameterList")
    suspend fun purchaseShippingLabel(
        site: SiteModel,
        orderId: Long,
        shippableItems: List<Long>,
        selectedPackage: PackageData,
        shipmentId: Int,
        shipTo: Address,
        shipFrom: OriginShippingAddress,
        selectedRate: WooShippingSelectedRateModel,
        weight: Float,
        lastOrderCompleted: Boolean,
        customsData: CustomsData?,
        hazmatSelection: ShippingLabelHazmatCategory? = null
    ): WooResult<PurchasedLabelData> {
        return restClient.purchaseShippingLabel(
            site = site,
            orderId = orderId,
            origin = mapper.toOriginAddressPurchaseDTO(shipFrom),
            destination = mapper.toDestinationAddressDTO(shipTo),
            selectedPackage = mapper.toPackagePurchaseDTO(
                shipmentId = shipmentId,
                selectedPackage = selectedPackage,
                selectedRate = selectedRate,
                shippableItems = shippableItems,
                weight = weight
            ),
            selectedRate = mapper.toRateDTO(selectedRate.rate),
            parentRate = selectedRate.parentRate?.let { mapper.toRateDTO(it) },
            selectedRateOptions = mapper.toSelectedRateOptions(selectedRate),
            customs = customsData?.let { mapper.toCustomsDTO(it) },
            hazmat = mapper.toHazmatDTO(hazmatSelection),
            lastOrderCompleted = lastOrderCompleted
        ).also {
            it.result?.let { purchaseResultDTO ->
                wooShippingDao.insertLabels(
                    mapper.invoke(purchaseResultDTO, site, orderId)
                )
            }
        }.asWooResult { mapper(it) }
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

        return if (normalizedAddress.result?.success == true || normalizedAddress.result?.errors != null) {
            normalizedAddress.asWooResult { mapper(it) }
        } else {
            WooResult(
                WooError(
                    type = WooErrorType.API_ERROR,
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
        isVerified: Boolean
    ): WooResult<DestinationShippingAddress> {
        val updatedAddress = restClient.updateDestinationAddress(
            site = site,
            orderId = orderId,
            address = mapper.toAddressDTO(address = address, isVerified = isVerified)
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

    suspend fun updateShipments(
        site: SiteModel,
        orderId: Long,
        shipments: ShipmentMap,
        shipmentIdsToUpdate: Map<String, Int>
    ) = restClient.updateShipments(
        site = site,
        orderId = orderId,
        shipments = shipments,
        shipmentIdsToUpdate = shipmentIdsToUpdate
    ).asWooResult().also {
        it.model?.data?.let { updatedShipments ->
            wooShippingDao.replaceShipments(
                mapper(updatedShipments, site, orderId)
            )
        }
    }

    suspend fun refundLabel(
        site: SiteModel,
        orderId: Long,
        labelId: Long
    ): WooResult<RefundLabelResponseDTO> = restClient.refundShippingLabel(orderId, labelId, site).asWooResult()

    suspend fun getShipments(orderId: Long): ShipmentMap {
        return mapper(wooShippingDao.getShipments(selectedSite.get().localId(), LocalOrRemoteId.RemoteId(orderId)))
    }

    suspend fun getLabels(
        orderId: Long
    ) = wooShippingDao.getLabels(
        selectedSite.get().localId(),
        LocalOrRemoteId.RemoteId(orderId)
    ).map { mapper(it) }

    suspend fun getPurchasedLabels(
        orderId: Long
    ) = wooShippingDao.getPurchasedLabels(
        selectedSite.get().localId(),
        LocalOrRemoteId.RemoteId(orderId)
    ).map { mapper(it) }

    fun observeLabel(
        orderId: Long,
        labelId: Long
    ) = wooShippingDao.observeLabel(
        selectedSite.get().localId(),
        LocalOrRemoteId.RemoteId(orderId),
        labelId
    ).map { entity -> entity?.let { mapper(it) } }
}
