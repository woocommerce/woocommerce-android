package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.printing.ShippingLabelPrintingResponse
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.DestinationAddressDTO
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class WooShippingLabelRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
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
        markOrderComplete: Boolean,
        hazmat: HazmatDTO = HazmatDTO(),
    ): WooPayload<PurchasedShippingLabelResponseDTO> {
        val url = "/wcshipping/v1/label/purchase/$orderId/"
        return wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = mapOf(
                "async" to true,
                "origin" to origin,
                "destination" to destination,
                "packages" to listOf(selectedPackage),
                "selected_rate" to mapOf(
                    selectedPackage.boxId to mapOf(
                        "rate" to selectedRate,
                        "parent" to null
                    )
                ),
                // TODO: `selected_rate_options` will be updated while adding UPS support PaJDVv-2Gf-p2
                "selected_rate_options" to "",
                "hazmat" to mapOf(selectedPackage.boxId to hazmat),
                "customs" to emptyMap<String, String>(),
                "user_meta" to mapOf("last_order_completed" to markOrderComplete)
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
            body = mapOf(
                "address" to address.copy(isVerified = true),
                "isVerified" to true // We always verify the address before saving it
            ),
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
}
