package com.woocommerce.android.ui.orders.details.editing.address

import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location

val testCountry = Location(
    code = "US",
    name = "USA",
)

val testState = AmbiguousLocation.Defined(
    Location(
        parentCode = "US",
        code = "CA",
        name = "California",
    )
)
