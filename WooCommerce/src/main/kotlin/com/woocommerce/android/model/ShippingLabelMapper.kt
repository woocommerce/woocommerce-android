package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import java.util.*
import javax.inject.Inject

class ShippingLabelMapper @Inject constructor(private val addressMapper: ShippingLabelAddressMapper) {
    fun toAppModel(databaseEntity: WCShippingLabelModel): ShippingLabel {
        return ShippingLabel(
            id = databaseEntity.remoteShippingLabelId,
            trackingNumber = databaseEntity.trackingNumber,
            carrierId = databaseEntity.carrierId,
            serviceName = databaseEntity.serviceName,
            status = databaseEntity.status,
            createdDate = databaseEntity.dateCreated?.let { Date(it) },
            expiryDate = databaseEntity.expiryDate?.let { Date(it) },
            packageName = databaseEntity.packageName,
            rate = databaseEntity.rate.toBigDecimal(),
            refundableAmount = databaseEntity.refundableAmount.toBigDecimal(),
            currency = databaseEntity.currency,
            productNames = databaseEntity.getProductNameList().map { it.trim() },
            productIds = databaseEntity.getProductIdsList(),
            originAddress = databaseEntity.getOriginAddress()?.let { addressMapper.toAppModel(it) },
            destinationAddress = databaseEntity.getDestinationAddress()?.let { addressMapper.toAppModel(it) },
            refund = databaseEntity.getRefundModel()?.toAppModel(),
            commercialInvoiceUrl = databaseEntity.commercialInvoiceUrl
        )
    }
}
