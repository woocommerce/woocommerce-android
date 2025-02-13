package org.wordpress.android.fluxc.model.shippinglabels

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.FormData
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelStatusApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelApiResponse
import javax.inject.Inject

class WCShippingLabelMapper
@Inject constructor() {
    fun map(response: ShippingLabelApiResponse, site: SiteModel): List<WCShippingLabelModel> {
        return response.labelsData?.map { labelItem ->
            WCShippingLabelModel().apply {
                remoteShippingLabelId = labelItem.labelId ?: 0L
                trackingNumber = labelItem.trackingNumber ?: ""
                carrierId = labelItem.carrierId ?: ""
                serviceName = labelItem.serviceName ?: ""
                status = labelItem.status ?: ""
                packageName = labelItem.packageName ?: ""
                rate = labelItem.rate?.toFloat() ?: 0F
                refundableAmount = labelItem.refundableAmount?.toFloat() ?: 0F
                currency = labelItem.currency ?: ""
                productNames = labelItem.productNames.toString()
                productIds = labelItem.productIds.toString()
                refund = labelItem.refund.toString()
                commercialInvoiceUrl = labelItem.commercialInvoiceUrl
                dateCreated = labelItem.dateCreated
                expiryDate = labelItem.expiryDate
                remoteOrderId = response.orderId ?: 0L
                formData = response.formData.toString()

                localSiteId = site.id
            }
        } ?: emptyList()
    }

    fun map(
        response: ShippingLabelStatusApiResponse,
        orderId: Long,
        origin: ShippingLabelAddress,
        destination: ShippingLabelAddress,
        site: SiteModel
    ): List<WCShippingLabelModel> {
        val gson = Gson()
        return response.labels?.map { labelItem ->
            WCShippingLabelModel().apply {
                remoteShippingLabelId = labelItem.labelId ?: 0L
                trackingNumber = labelItem.trackingNumber ?: ""
                carrierId = labelItem.carrierId ?: ""
                serviceName = labelItem.serviceName ?: ""
                status = labelItem.status ?: ""
                packageName = labelItem.packageName ?: ""
                rate = labelItem.rate?.toFloat() ?: labelItem.refundableAmount?.toFloat() ?: 0F
                refundableAmount = labelItem.refundableAmount?.toFloat() ?: 0F
                currency = labelItem.currency ?: ""
                productNames = labelItem.productNames.toString()
                productIds = labelItem.productIds.toString()
                refund = labelItem.refund.toString()
                commercialInvoiceUrl = labelItem.commercialInvoiceUrl
                dateCreated = labelItem.dateCreated
                expiryDate = labelItem.expiryDate

                remoteOrderId = orderId
                formData = gson.toJson(FormData(origin = origin, destination = destination))

                localSiteId = site.id
            }
        } ?: emptyList()
    }
}
