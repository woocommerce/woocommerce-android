package com.woocommerce.android.ui.addressformatting

import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode

// WooCommerce countries whose state codes are all or mostly numeric (e.g. CN24), so the code means nothing to a reader
private val COUNTRIES_DISPLAYING_STATE_NAME = setOf(
    "AL", "BD", "BG", "CN", "DO", "DZ", "HR", "JM", "JP", "KE", "KN",
    "PA", "PY", "RS", "TH", "TR", "TZ", "UA", "UG", "UM", "ZM",
)

fun displaysStateName(countryCode: LocationCode): Boolean = countryCode in COUNTRIES_DISPLAYING_STATE_NAME

fun AmbiguousLocation.presentationName(countryCode: LocationCode): String =
    this.asLocation().run {
        if (displaysStateName(countryCode)) {
            name
        } else {
            code
        }
    }
