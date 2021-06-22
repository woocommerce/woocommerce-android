package com.woocommerce.android.model

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import org.wordpress.android.fluxc.model.WCMetaData
import java.io.Serializable

// we can's use Parcelize here for the generic type due to this bug: https://youtrack.jetbrains.com/issue/KT-42652
data class MetaData<T : Any>(
    val key: String,
    val value: T
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        readValueFromParcel(parcel)
    )

    init {
        if (value !is Parcelable && value !is Serializable) throw IllegalArgumentException()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(key)
        parcel.writeSerializable(value::class.java)
        parcel.writeValue(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Creator<MetaData<*>> {
        override fun createFromParcel(parcel: Parcel): MetaData<*> {
            return MetaData<Any>(parcel)
        }

        override fun newArray(size: Int): Array<MetaData<*>?> {
            return arrayOfNulls(size)
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> readValueFromParcel(parcel: Parcel): T {
            val classLoader = (parcel.readSerializable() as Class<*>).classLoader
            return parcel.readValue(classLoader) as T
        }
    }
}

/**
 * Try to convert [WCMetaData] to [MetaData]
 * The function prefers [WCMetaData.displayKey] and [WCMetaData.displayValue] over the other variants,
 * and tries to cast them to the generic type [T], or convert it using the optional [valueConverter]
 * If it fails, it will return null
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> WCMetaData.toAppModel(valueConverter: ((Any) -> T)? = null): MetaData<T>? {
    val value = valueConverter?.let { converter ->
        displayValue?.let(converter) ?: converter(value)
    } ?: displayValue as? T ?: value as? T ?: return null
    return MetaData(
        key = displayKey ?: key,
        value = value
    )
}
