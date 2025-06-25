package org.wordpress.android.fluxc.wc.shippinglabels

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.AccountSettingsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelStatusApiResponse

object WCShippingLabelTestUtils {
    fun generateSampleShippingLabel(
        remoteId: Long,
        orderId: Long = 12,
        siteId: Int = 6,
        carrierId: String = "usps",
        serviceName: String = "USPS - Priority Mail",
        status: String = "PURCHASED",
        packageName: String = "Small Flat Rate Box",
        rate: Float = 7.65F,
        refundableAmount: Float = 7.65F,
        currency: String = "USD",
        refund: String? = null,
        productNames: String = "[Woo T-shirt, Herman Chair]",
        productIds: String = "[60, 61, 62]"
    ): WCShippingLabelModel {
        return WCShippingLabelModel(
            localSiteId = LocalId(siteId),
            remoteOrderId = RemoteId(orderId),
            remoteShippingLabelId = RemoteId(remoteId),
            carrierId = carrierId,
            serviceName = serviceName,
            packageName = packageName,
            status = status,
            rate = rate,
            refundableAmount = refundableAmount,
            currency = currency,
            productNames = productNames,
            productIds = productIds,
            refund = refund.orEmpty(),
            trackingNumber = "",
            dateCreated = 0,
            expiryDate = 0,
            formData = "",
            commercialInvoiceUrl = null
        )
    }

    fun generateSampleShippingLabelApiResponse(): ShippingLabelApiResponse? {
        val json = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/shipping-labels.json")
        val responseType = object : TypeToken<ShippingLabelApiResponse>() {}.type
        return Gson().fromJson(json, responseType) as? ShippingLabelApiResponse
    }

    fun generateSamplePrintShippingLabelApiResponse(): ShippingLabelRestClient.PrintShippingLabelApiResponse? {
        val json = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/print-shipping-labels.json")
        val responseType = object : TypeToken<ShippingLabelRestClient.PrintShippingLabelApiResponse>() {}.type
        return Gson().fromJson(json, responseType) as? ShippingLabelRestClient.PrintShippingLabelApiResponse
    }

    fun generateSampleGetPackagesApiResponse(): ShippingLabelRestClient.GetPackageTypesResponse? {
        val json = UnitTestUtils.getStringFromResourceFile(
                this.javaClass,
                "wc/shipping-labels-packages.json"
        )
        val responseType = object : TypeToken<ShippingLabelRestClient.GetPackageTypesResponse>() {}.type
        return Gson().fromJson(json, responseType) as? ShippingLabelRestClient.GetPackageTypesResponse
    }

    fun generateSampleGetShippingRatesApiResponse(): ShippingLabelRestClient.ShippingRatesApiResponse? {
        val json = UnitTestUtils.getStringFromResourceFile(
                this.javaClass,
                "wc/shipping-labels-carriers.json"
        )
        val responseType = object : TypeToken<ShippingLabelRestClient.ShippingRatesApiResponse>() {}.type
        return Gson().fromJson(json, responseType) as? ShippingLabelRestClient.ShippingRatesApiResponse
    }

    fun generateSampleAccountSettingsApiResponse(): AccountSettingsApiResponse {
        val json = UnitTestUtils.getStringFromResourceFile(
                this.javaClass,
                "wc/shipping-labels-account-settings.json"
        )
        return Gson().fromJson(json, AccountSettingsApiResponse::class.java)
    }

    fun generateSamplePurchaseShippingLabelsApiResponse(): ShippingLabelStatusApiResponse {
        val json = UnitTestUtils.getStringFromResourceFile(
                this.javaClass,
                "wc/purchase-shipping-labels.json"
        )
        return Gson().fromJson(json, ShippingLabelStatusApiResponse::class.java)
    }

    fun generateSampleShippingLabelsStatusApiResponse(done: Boolean): ShippingLabelStatusApiResponse {
        val json = UnitTestUtils.getStringFromResourceFile(
                this.javaClass,
                "wc/status-shipping-labels-${if (done) 2 else 1}.json"
        )
        return Gson().fromJson(json, ShippingLabelStatusApiResponse::class.java)
    }

    fun generateErrorShippingLabelsStatusApiResponse(): ShippingLabelStatusApiResponse {
        val json = UnitTestUtils.getStringFromResourceFile(
                this.javaClass,
                "wc/status-shipping-labels-error.json"
        )
        return Gson().fromJson(json, ShippingLabelStatusApiResponse::class.java)
    }
}
