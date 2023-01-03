package com.woocommerce.android.ui.addressformatting

import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode

object AdministrativeAreaAdapter {

    fun determineAreaForLibAddressInputLibrary(countryCode: LocationCode, adminArea: Location): String =
        if (countryCode == "JP" || countryCode == "TR") {
            adminArea.name
        } else {
            adminArea.code
        }
}
