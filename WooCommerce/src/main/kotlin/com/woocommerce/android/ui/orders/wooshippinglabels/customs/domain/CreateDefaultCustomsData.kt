package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import com.woocommerce.android.ui.orders.wooshippinglabels.customs.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsItem
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.RestrictionType
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

class CreateDefaultCustomsData @Inject constructor(
    private val dataStore: WCDataStore
) {
    suspend operator fun invoke(items: List<ShippableItemModel>, originAddress: OriginShippingAddress): CustomsData {
        val defaultOriginCountry = dataStore.getCountry(originAddress.country)

        return CustomsData(
            contentType = ContentType.MERCHANDISE,
            contentDescription = "",
            restrictionType = RestrictionType.NONE,
            restrictionDescription = "",
            isReturnToSender = false,
            itn = "",
            items = items.map { it.createDefaultCustomsItem(defaultOriginCountry) }
        )
    }

    private fun ShippableItemModel.createDefaultCustomsItem(defaultOriginCountry: WCLocationModel?) = CustomsItem(
        productID = productId,
        description = title,
        quantity = quantity,
        value = price,
        weight = weight,
        hsTariffNumber = "",
        originCountry = defaultOriginCountry?.name.orEmpty(),
        originCountryCode = defaultOriginCountry?.code.orEmpty()
    )
}
