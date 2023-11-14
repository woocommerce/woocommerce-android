package com.woocommerce.android.ui.addressformatting

import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode

fun AmbiguousLocation.presentationName(countryCode: LocationCode): String =
    this.asLocation().run {
        if (countryCode == "JP" || countryCode == "TR") {
            name
        } else {
            code
        }
    }
