package com.woocommerce.android.widgets

import android.os.Parcel
import android.os.Parcelable
import android.view.View.BaseSavedState
import com.woocommerce.android.extensions.parcelable

/**
 * Wrapper for custom view state which can be used to save the parent's (super.onSaveInstanceState) state
 * or any child view's state
 */
class WCSavedState : BaseSavedState {
    var savedState: Parcelable? = null
        private set

    constructor(superState: Parcelable?, inState: Parcelable) : super(superState) {
        savedState = inState
    }

    constructor(source: Parcel) : super(source) {
        savedState = source.parcelable(this::class.java.classLoader)
    }

    constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
        savedState = loader?.let {
            source.parcelable(it)
        } ?: source.parcelable(this::class.java.classLoader)
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeParcelable(savedState, 0)
    }

    companion object {
        @Suppress("UNUSED")
        @JvmField
        val CREATOR = object : Parcelable.ClassLoaderCreator<WCSavedState> {
            override fun createFromParcel(source: Parcel, loader: ClassLoader?): WCSavedState {
                return WCSavedState(source, loader)
            }

            override fun createFromParcel(source: Parcel): WCSavedState {
                return WCSavedState(source)
            }

            override fun newArray(size: Int): Array<WCSavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}
