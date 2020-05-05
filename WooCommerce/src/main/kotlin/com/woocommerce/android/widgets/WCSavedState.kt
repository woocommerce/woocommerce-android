package com.woocommerce.android.widgets

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Parcel
import android.os.Parcelable
import android.view.View.BaseSavedState
import androidx.annotation.RequiresApi

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

    /**
     * Workaround to differentiate between this method and the one that requires API 24+ because
     * the super(source, loader) method won't work on older APIs - thus the app will crash.
     */
    constructor(source: Parcel, loader: ClassLoader?, superState: Parcelable?): super(superState) {
        savedState = source.readParcelable(loader)
    }

    constructor(source: Parcel) : super(source) {
        savedState = source.readParcelable(this::class.java.classLoader)
    }

    @RequiresApi(VERSION_CODES.N)
    constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
        savedState = loader?.let {
            source.readParcelable<Parcelable>(it)
        } ?: source.readParcelable<Parcelable>(this::class.java.classLoader)
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
                return if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    WCSavedState(source, loader)
                } else {
                    WCSavedState(source, loader, source.readParcelable<Parcelable>(loader))
                }
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
