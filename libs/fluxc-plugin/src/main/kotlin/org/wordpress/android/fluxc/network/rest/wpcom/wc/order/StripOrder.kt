package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.CHARGE_ID_KEY
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.RECEIPT_URL_KEY
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.SHIPPING_PHONE_KEY
import javax.inject.Inject

internal class StripOrder @Inject constructor(private val gson: Gson) {
    operator fun invoke(fatModel: OrderEntity): OrderEntity {
        return fatModel.copy(
                lineItems = gson.toJson(fatModel.getLineItemList().map { lineItemDto: LineItem ->
                    lineItemDto.copy(
                        metaData = lineItemDto.metaData
                            ?.filter {
                                it.isDisplayable
                                    || it.key == WCMetaData.BundleMetadataKeys.BUNDLED_ITEM_ID
                            }
                    )
                }),
                shippingLines = gson.toJson(fatModel.getShippingLineList()),
                feeLines = gson.toJson(fatModel.getFeeLineList()),
                taxLines = gson.toJson(fatModel.getTaxLineList()),
                metaData = fatModel.metaData
                        .filter {
                            it.key == CHARGE_ID_KEY ||
                                    it.key == SHIPPING_PHONE_KEY ||
                                    it.key == RECEIPT_URL_KEY
                        }
                )
    }
}
