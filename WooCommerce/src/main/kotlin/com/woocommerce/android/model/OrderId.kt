package com.woocommerce.android.model

import android.os.Parcel
import android.os.Parcelable
import org.wordpress.android.fluxc.model.LocalOrRemoteId

// Can not use @Parcelize due to the bug - https://issuetracker.google.com/issues/177856519
@JvmInline
value class OrderId(val value: Long) : Parcelable {
    override fun toString() = value.toString()
    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel?, flags: Int) {
        parcel?.writeLong(value)
    }

    companion object {
        val CREATOR: Parcelable.Creator<OrderId> = object : Parcelable.Creator<OrderId> {
            override fun createFromParcel(parcel: Parcel) = OrderId(parcel.readLong())

            override fun newArray(size: Int): Array<OrderId?> = arrayOfNulls(size)
        }
    }
}

fun OrderId.toFluxcRemoteId() = LocalOrRemoteId.RemoteId(value)
