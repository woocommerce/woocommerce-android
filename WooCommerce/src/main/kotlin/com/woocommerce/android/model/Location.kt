package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.data.WCLocationModel

@Parcelize
data class Location(
    val code: LocationCode,
    val name: String,
    val parentCode: String = ""
) : Parcelable {
    companion object {
        val EMPTY = Location("", "")
    }
}

fun WCLocationModel.toAppModel(): Location {
    return Location(
        code = code,
        name = name,
        parentCode = parentCode
    )
}
