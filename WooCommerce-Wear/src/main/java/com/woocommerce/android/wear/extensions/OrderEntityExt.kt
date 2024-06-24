package com.woocommerce.android.wear.extensions

import com.woocommerce.commons.WearOrder
import com.woocommerce.commons.WearOrderAddress
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.order.OrderAddress

fun OrderEntity.toWearOrder() = WearOrder(
    localSiteId = localSiteId.value,
    id = orderId,
    date = dateCreated,
    number = number,
    total = total,
    status = status,
    billingFirstName = billingFirstName,
    billingLastName = billingLastName,
    address = getBillingAddress().toWearOrderAddress(),
    lineItemsJson = lineItems
)

fun WearOrder.toOrderEntity() = OrderEntity(
    orderId = id,
    localSiteId = LocalOrRemoteId.LocalId(localSiteId),
    dateCreated = date,
    number = number,
    total = total,
    status = status,
    billingFirstName = billingFirstName,
    billingLastName = billingLastName,
    lineItems = lineItemsJson
)

fun OrderAddress.Billing.toWearOrderAddress() = WearOrderAddress(
    email = email,
    firstName = firstName,
    lastName = lastName,
    company = company,
    address1 = address1,
    address2 = address2,
    city = city,
    state = state,
    postcode = postcode,
    country = country,
    phone = phone
)
