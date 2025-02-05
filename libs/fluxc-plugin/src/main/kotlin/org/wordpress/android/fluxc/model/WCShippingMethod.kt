package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.persistence.entity.ShippingMethodEntity

data class WCShippingMethod (
    val id: String,
    val title: String
)

fun ShippingMethodEntity.toAppModel() = WCShippingMethod(this.id,this.title)
