package com.woocommerce.android.widgets

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.InputFilter
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.AttrRes
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.view_material_outlined_edittext.view.*

/**
 * Custom View that encapsulates a [TextInputLayout] and [TextInputEditText], and as such has the following
 * capabilities:
 * - Show helper text
 * - Show error text
 * - Enable/set counter and maxLength
 * - Set the text on the child [TextInputEditText]
 * - Set the selected text on the child [TextInputEditText]
 *
 * The entire view acts as a single component. The following attributes have been exposed to this view as
 * custom attributes and are used to interact with the nested [TextInputEditText] component:
 * - [android:inputType]
 * - [android:maxLength]
 * - [android:enabled]
 * - [android:text]
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

    fun setText(selectedText: String) {
        edit_text.setText(selectedText)
    }

    fun getText() = edit_text.text.toString()

    fun setSelection(start: Int, stop: Int) {
        edit_text.setSelection(start, stop)
    }

    fun setOnTextChangedListener(cb: (text: Editable?) -> Unit) {
        edit_text.doAfterTextChanged {
            clearError()
            cb(it)
        }
    }

    fun setOnEditorActionListener(cb: (text: String) -> Boolean) {
        edit_text.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                val text = edit_text.text.toString()
                if (text.isNotEmpty()) {
                    edit_text.setText("")
                    cb.invoke(text)
                } else false
            } else {
                false
            }
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

        edit_text?.isEnabled = enabled
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        edit_text.onSaveInstanceState()?.let {
            bundle.putParcelable(KEY_SUPER_STATE, WCSavedState(super.onSaveInstanceState(), it))
        }
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = (state as? Bundle)?.getParcelable<WCSavedState>(KEY_SUPER_STATE)?.let {
            restoreViewState(it)
        } ?: state
        super.onRestoreInstanceState(bundle)
    }

    private fun restoreViewState(state: WCSavedState): Parcelable {
        edit_text.onRestoreInstanceState(state.savedState)
        return state.superState
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        super.dispatchThawSelfOnly(container)
    }
}
