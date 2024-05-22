package com.woocommerce.android.extensions

import com.woocommerce.commons.WearOrder
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.OrderEntity

fun OrderEntity.toWearOrder() = WearOrder(
    localSiteId = localSiteId.value,
    id = orderId,
    date = dateCreated,
    number = number,
    total = total,
    status = status,
    billingFirstName = billingFirstName,
    billingLastName = billingLastName
)

fun WearOrder.toOrderEntity() = OrderEntity(
    orderId = id,
    localSiteId = LocalOrRemoteId.LocalId(localSiteId),
    dateCreated = date,
    number = number,
    total = total,
    status = status,
    billingFirstName = billingFirstName,
    billingLastName = billingLastName
)
