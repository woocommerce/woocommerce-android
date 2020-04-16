package com.woocommerce.android.widgets

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.view_material_outlined_spinner.view.*

/**
 * Custom View that mimics a TextInputEditText with a spinner that opens a selector dialog it.
 * This view will display a text box which will open a dialog when clicked.
 * The entire view acts as a single component.
 */
class WCMaterialOutlinedSpinnerView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.wcMaterialOutlinedSpinnerViewStyle
) : TextInputLayout(ctx, attrs, defStyleAttr) {
    companion object {
        private const val KEY_SUPER_STATE = "WC-OUTLINED-SPINNER-VIEW-SUPER-STATE"
    }
    init {
        View.inflate(context, R.layout.view_material_outlined_spinner, this)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.WCMaterialOutlinedSpinnerView)
            try {
                // Set the edit text spinner hint
                a.getString(R.styleable.WCMaterialOutlinedSpinnerView_android_text)?.let {
                    setText(it)
                }

                isEnabled = a.getBoolean(R.styleable.WCMaterialOutlinedSpinnerView_android_enabled, true)
            } finally {
                a.recycle()
            }
        }
    }

    fun setClickListener(onClickListener: ((view: View) -> Unit)) {
        spinner_edit_text.setOnClickListener(onClickListener)
    }

    fun setText(selectedText: String) {
        spinner_edit_text.setText(selectedText)
    }

    fun getText() = spinner_edit_text.text.toString()

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        spinner_edit_text.isEnabled = enabled
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        spinner_edit_text.onSaveInstanceState()?.let {
            bundle.putParcelable(KEY_SUPER_STATE, SavedState(super.onSaveInstanceState(), it))
        }
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = (state as? Bundle)?.getParcelable<SavedState>(KEY_SUPER_STATE)?.let {
            restoreViewState(it)
        } ?: state
        super.onRestoreInstanceState(bundle)
    }

    private fun restoreViewState(state: SavedState): Parcelable {
        spinner_edit_text.onRestoreInstanceState(state.editTextState)
        return state.superState
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchThawSelfOnly(container)
    }

    internal class SavedState : BaseSavedState {
        internal var editTextState: Parcelable? = null

        constructor(superState: Parcelable?, inEditTextState: Parcelable) : super(superState) {
            editTextState = inEditTextState
        }

        /**
         * Workaround to differentiate between this method and the one that requires API 24+ because
         * the super(source, loader) method won't work on older APIs - thus the app will crash.
         */
        constructor(source: Parcel, loader: ClassLoader?, superState: Parcelable?): super(superState) {
            editTextState = source.readParcelable<Parcelable>(loader)
        }

        constructor(source: Parcel) : super(source) {
            editTextState = source.readParcelable(this::class.java.classLoader)
        }

        @RequiresApi(VERSION_CODES.N)
        constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
            editTextState = loader?.let {
                source.readParcelable<Parcelable>(it)
            } ?: source.readParcelable<Parcelable>(this::class.java.classLoader)
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(editTextState, 0)
        }

        companion object {
            @Suppress("UNUSED")
            @JvmField
            val CREATOR = object : Parcelable.ClassLoaderCreator<SavedState> {
                override fun createFromParcel(source: Parcel, loader: ClassLoader?): SavedState {
                    return if (VERSION.SDK_INT >= VERSION_CODES.N) {
                        SavedState(source, loader)
                    } else {
                        SavedState(source, loader, source.readParcelable<Parcelable>(loader))
                    }
                }

                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
