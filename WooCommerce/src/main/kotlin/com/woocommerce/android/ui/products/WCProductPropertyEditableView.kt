package com.woocommerce.android.ui.products

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import com.woocommerce.android.R

class WCProductPropertyEditableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private val view = View.inflate(context, R.layout.product_property_editable_view_layout, this)
    private val editableText = view.findViewById<EditText>(R.id.editText)

    // Flag to check if [EditText] already has a [EditText.doAfterTextChanged] defined to avoid multiple callbacks
    private var isTextChangeListenerActive: Boolean = false

    fun show(hint: String, detail: String?, isFocused: Boolean, isReadOnly: Boolean) {
        editableText.hint = hint

        if (!detail.isNullOrEmpty() && detail != editableText.text.toString()) {
            editableText.setText(detail)
            editableText.setSelection(detail.length)
        }

        editableText.isEnabled = !isReadOnly

        if (isFocused) {
            editableText.requestFocus()
        }
    }

    fun setOnTextChangedListener(cb: (text: Editable?) -> Unit) {
        if (!isTextChangeListenerActive) {
            isTextChangeListenerActive = true
            editableText.doAfterTextChanged { cb(it) }
        }
    }
}
