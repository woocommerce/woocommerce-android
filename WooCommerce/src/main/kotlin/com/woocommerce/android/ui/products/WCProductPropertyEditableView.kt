package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.woocommerce.android.R

class WCProductPropertyEditableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private var editableText: EditText

    init {
        with(View.inflate(context, R.layout.product_property_editable_view, this)) {
            editableText = findViewById(R.id.editText)
        }
    }

    fun show(hint: String, detail: String?) {
        if (detail.isNullOrEmpty()) {
            editableText.hint = hint
        } else {
            editableText.setText(detail)
        }
    }
}
