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

sealed class AmbiguousLocation : Parcelable {
    @Parcelize
    data class Defined(val value: Location) : AmbiguousLocation() {
        override fun isNotEmpty() = this.value != Location.EMPTY
    }

    @Parcelize
    data class Raw(val value: String) : AmbiguousLocation() {
        override fun isNotEmpty() = this.value.isNotBlank()
    }

    abstract fun isNotEmpty(): Boolean

    val codeOrRaw
        get() = when (this) {
            is Defined -> this.value.code
            is Raw -> this.value
        }

    fun asLocation(): Location = when (this) {
        is Defined -> this.value
        is Raw -> Location(this.value, this.value)
    }

    companion object {
        val EMPTY = Raw("")
    }
}

fun WCLocationModel.toAppModel(): Location {
    return Location(
        code = code,
        name = name,
        parentCode = parentCode
    )
}
