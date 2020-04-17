package com.woocommerce.android.widgets

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.InputFilter
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.AttrRes
import androidx.annotation.RequiresApi
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.view_material_outlined_edittext.view.*

/**
 * Custom View that mimics a TextInputEditText with a summary TextView below it. This view will display
 * a text box and a summary. The entire view acts as a single component.
 *
 */
class WCMaterialOutlinedEditTextView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.wcMaterialOutlinedEditTextViewStyle
) : TextInputLayout(ctx, attrs, defStyleAttr) {
    companion object {
        private const val KEY_SUPER_STATE = "WC-OUTLINED-EDITTEXT-VIEW-SUPER-STATE"
    }
    init {
        View.inflate(context, R.layout.view_material_outlined_edittext, this)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.WCMaterialOutlinedEditTextView
            )
            try {
                // Set the edit text input type
                edit_text.inputType = a.getInt(
                        R.styleable.WCMaterialOutlinedEditTextView_android_inputType,
                        EditorInfo.TYPE_TEXT_VARIATION_NORMAL
                )

                // Set the max length
                if (a.hasValue(R.styleable.WCMaterialOutlinedEditTextView_android_maxLength)) {
                    val max = a.getInt(R.styleable.WCMaterialOutlinedEditTextView_android_maxLength, 0)
                    setMaxLength(max)
                }

                // Set the startup text
                a.getString(R.styleable.WCMaterialOutlinedSpinnerView_android_text)?.let {
                    setText(it)
                }

                isEnabled = a.getBoolean(R.styleable.WCMaterialOutlinedCurrencyEditTextView_android_enabled, true)
            } finally {
                a.recycle()
            }
        }
    }

    fun getText() = edit_text.editableText.toString()

    fun setText(selectedText: String) {
        edit_text.setText(selectedText)
    }

    fun setOnTextChangedListener(cb: (text: Editable?) -> Unit) {
        edit_text.doAfterTextChanged {
            clearError()
            cb(it)
        }
    }

    fun clearError() {
        error = null
    }

    fun setMaxLength(max: Int) {
        edit_text.filters += InputFilter.LengthFilter(max)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        edit_text.isEnabled = enabled
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        edit_text.onSaveInstanceState()?.let {
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
        edit_text.onRestoreInstanceState(state.editTextState)
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
