package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import com.google.gson.JsonObject
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.printing.ShippingLabelPrintingResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.DestinationAddressDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.ShippingRateSurchargeDTO
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class WooShippingLabelRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
    suspend fun fetchShippingEligibility(site: SiteModel, orderId: Long): WooPayload<EligibilityResponse> {
        val url = "/wcshipping/v1/eligibility/$orderId"

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = mapOf(
                "can_create_customs_form" to true.toString(),
                "can_create_package" to true.toString(),
                "can_create_payment_method" to true.toString()
            ),
            clazz = EligibilityResponse::class.java,
        ).toWooPayload()
    }

    suspend fun fetchShippingLabelPrinting(
        site: SiteModel,
        labelIds: List<Long>,
        paperSize: String
    ): WooPayload<ShippingLabelPrintingResponse> {
        val url = "/wcshipping/v1/label/print"

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = mapOf(
                "label_id_csv" to labelIds.joinToString { "$it," },
                "paper_size" to paperSize
            ),
            clazz = ShippingLabelPrintingResponse::class.java,
        ).toWooPayload()
    }

    suspend fun fetchAccountSettings(
        site: SiteModel,
    ): WooPayload<AccountSettingsDTO> {
        val url = "/wcshipping/v1/account/settings/"

        val result = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = AccountSettingsDTO::class.java,
        )

        return result.toWooPayload()
    }

    suspend fun updateAccountSettings(
        site: SiteModel,
        selectedPaymentMethodId: Long?,
        emailReceipts: Boolean
    ): WooPayload<Unit> {
        val url = "/wcshipping/v1/account/settings/"

        val result = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = JsonObject::class.java,
            body = mapOf(
                "selected_payment_method_id" to selectedPaymentMethodId,
                "email_receipts" to emailReceipts,
                "enabled" to true // See WOOSHIP-1410
            ).filterNotNull()
        )

        val success = (result as? WPAPIResponse.Success<JsonObject>)?.data?.get("success")?.asBoolean ?: false

        return when {
            result is WPAPIResponse.Error -> WooPayload(error = result.error.toWooError())
            !success -> WooPayload(
                error = WooError(
                    type = WooErrorType.API_ERROR,
                    original = BaseRequest.GenericErrorType.UNKNOWN,
                    message = "Something went wrong"
                )
            )

            else -> WooPayload(Unit)
        }
    }

    suspend fun fetchConfig(site: SiteModel, orderId: Long): WooPayload<ConfigResponse> {
        val url = "/wcshipping/v1/config/label-purchase/$orderId"

        val result = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = ConfigResponse::class.java,
        )

        return result.toWooPayload()
    }

    suspend fun fetchPurchasedShippingLabels(
        site: SiteModel,
        orderId: Long,
    ): WooPayload<GetShippingLabelResponse> {
        val url = "/wcshipping/v1/label/purchase/$orderId/"

        val result = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = GetShippingLabelResponse::class.java,
        )

        return result.toWooPayload()
    }

    suspend fun fetchShippingLabelStatus(
        site: SiteModel,
        orderId: Long,
        labelId: Long,
    ): WooPayload<GetShippingLabelStatusResponse> {
        val url = "/wcshipping/v1/label/status/$orderId/$labelId/"

        val result = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = GetShippingLabelStatusResponse::class.java,
        )

        return result.toWooPayload()
    }

    @Suppress("LongParameterList", "ForbiddenComment")
    suspend fun purchaseShippingLabel(
        orderId: Long,
        site: SiteModel,
        origin: OriginAddressPurchaseDTO,
        destination: DestinationAddressDTO,
        selectedPackage: PackagePurchaseDTO,
        selectedRate: RateDTO,
        parentRate: RateDTO?,
        selectedRateOptions: Map<WooShippingRateModel.Option, ShippingRateSurchargeDTO>,
        lastOrderCompleted: Boolean,
        hazmat: HazmatDTO,
        customs: CustomsDTO?,
    ): WooPayload<PurchasedShippingLabelResponseDTO> {
        val url = "/wcshipping/v1/label/purchase/$orderId/"
        val shipmentKey = "shipment_${selectedPackage.id}"

        return wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = mapOf(
                "origin" to origin,
                "destination" to destination,
                // For this purchase endpoint, `id` represents the shipment ID instead of the package ID
                "packages" to listOf(selectedPackage),
                "selected_rate" to mapOf(
                    "rate" to selectedRate,
                    "parent" to parentRate
                ),
                "selected_rate_options" to selectedRateOptions.mapKeys { it.key.typeId },
                "hazmat" to mapOf(shipmentKey to hazmat),
                "customs" to customs?.let { mapOf(shipmentKey to it) }.orEmpty(),
                "user_meta" to mapOf("last_order_completed" to lastOrderCompleted),
                "features_supported_by_client" to listOf("upsdap", "fedex"),
            ),
            clazz = PurchasedShippingLabelResponseDTO::class.java,
        ).toWooPayload()
    }

    suspend fun fetchOriginAddresses(
        site: SiteModel,
    ): WooPayload<Array<AddressDTO>> {
        val url = "/wcshipping/v1/address/origins"
        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<AddressDTO>::class.java,
        ).toWooPayload()
    }

    suspend fun normalizeAddress(
        site: SiteModel,
        address: AddressDTO,
    ): WooPayload<NormalizationResponseDTO> {
        val url = "/wcshipping/v1/address/normalize/"

        val result = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = mapOf("address" to address),
            clazz = NormalizationResponseDTO::class.java,
        )

        return result.toWooPayload()
    }

    suspend fun updateOriginAddress(
        site: SiteModel,
        address: AddressDTO,
    ): WooPayload<UpdateAddressResponseDTO> {
        val url = "/wcshipping/v1/address/update_origin/"

        val result = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = mapOf(
                "address" to address.copy(isVerified = true),
                "isVerified" to true // We always verify the address before saving it
            ),
            clazz = UpdateAddressResponseDTO::class.java,
        )

        return result.toWooPayload()
    }

    suspend fun updateDestinationAddress(
        site: SiteModel,
        orderId: Long,
        address: AddressDTO,
    ): WooPayload<UpdateAddressResponseDTO> {
        val url = "/wcshipping/v1/address/$orderId/update_destination/"

        val result = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = mapOf("address" to address, "isVerified" to address.isVerified),
            clazz = UpdateAddressResponseDTO::class.java,
        )

        return result.toWooPayload()
    }

    suspend fun verifyDestinationAddress(
        site: SiteModel,
        orderId: Long,
    ): WooPayload<VerifyDestinationAddressResponseDTO> {
        val url = "/wcshipping/v1/address/$orderId/verify_order/"

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = VerifyDestinationAddressResponseDTO::class.java,
        ).toWooPayload()
    }

    /**
     * Update the shipments for a specific order.
     */
    suspend fun updateShipments(
        site: SiteModel,
        orderId: Long,
        shipments: ShipmentMap,
        shipmentIdsToUpdate: Map<String, Int>
    ): WooPayload<UpdateShipmentsResponse> {
        val url = "/wcshipping/v1/shipments/$orderId"

        val result = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = mapOf("shipments" to shipments, "shipmentIdsToUpdate" to shipmentIdsToUpdate),
            clazz = UpdateShipmentsResponse::class.java,
        )

        return result.toWooPayload()
    }

    suspend fun refundShippingLabel(
        orderId: Long,
        labelId: Long,
        site: SiteModel
    ): WooPayload<RefundLabelResponseDTO> {
        val url = "/wcshipping/v1/label/refund/$orderId/$labelId"
        return wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = RefundLabelResponseDTO::class.java,
        ).toWooPayload()
    }

    suspend fun updateUPSDAPAgreement(
        site: SiteModel,
        originAddress: OriginAddressPurchaseDTO,
        agreementAccepted: Boolean
    ): WooPayload<Unit> {
        val url = "/wcshipping/v1/carrier-strategy/upsdap"

        val result = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = mapOf(
                "origin" to mapOf(
                    "address" to originAddress.address,
                    "address_2" to originAddress.address2,
                    "city" to originAddress.city,
                    "company" to originAddress.company,
                    "country" to originAddress.country,
                    // The `name` field is required by the API, but it can't be blank
                    "name" to originAddress.name.orEmpty().ifBlank { "" },
                    "phone" to originAddress.phone,
                    "postcode" to originAddress.postcode,
                    "state" to originAddress.state,
                    "email" to originAddress.email
                ),
                "confirmed" to agreementAccepted
            ),
            clazz = JsonObject::class.java,
        )

        return if (result is WPAPIResponse.Error) {
            WooPayload(error = result.error.toWooError())
        } else {
            WooPayload(Unit)
        }
    }

    suspend fun updateFedExAgreement(
        site: SiteModel,
        agreementAccepted: Boolean
    ): WooPayload<Unit> {
        val url = "/wcshipping/v1/carrier-strategy/fedex"

        val result = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = mapOf("confirmed" to agreementAccepted),
            clazz = JsonObject::class.java,
        )

        return if (result is WPAPIResponse.Error) {
            WooPayload(error = result.error.toWooError())
        } else {
            WooPayload(Unit)
        }
    }
}
