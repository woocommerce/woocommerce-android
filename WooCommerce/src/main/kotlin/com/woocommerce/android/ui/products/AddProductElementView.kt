package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.use
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.add_product_element_view.view.*

class AddProductElementView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.add_product_element_view, this)
        context.obtainStyledAttributes(attrs, R.styleable.AddProductElementView, 0, 0).use { typedArray ->
            val buttonText = typedArray.getString(R.styleable.AddProductElementView_buttonText)
            if (buttonText.isNullOrEmpty()) {
                throw IllegalArgumentException("AddProductElementView must have a text for its button (buttonText)")
            }
            addElementButton.text = buttonText
        }
    }

    fun initView(callback: (View) -> Unit) {
        addElementButton.setOnClickListener { view -> callback(view) }
    }
}
