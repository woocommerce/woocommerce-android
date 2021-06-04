package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.data.WCLocationModel

@Parcelize
data class Location(
    val code: String,
    val name: String,
    val parentCode: String = ""
) : Parcelable

fun WCLocationModel.toAppModel(): Location {
    return Location(
        code = code,
        name = name,
        parentCode = parentCode
    )
}
