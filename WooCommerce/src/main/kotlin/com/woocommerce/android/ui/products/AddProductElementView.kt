package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.res.use
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AddProductElementViewBinding

class AddProductElementView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = AddProductElementViewBinding.inflate(LayoutInflater.from(ctx), this)

    init {
        context.obtainStyledAttributes(attrs, R.styleable.AddProductElementView, 0, 0).use { typedArray ->
            val buttonText = typedArray.getString(R.styleable.AddProductElementView_buttonText)
            if (buttonText.isNullOrEmpty()) {
                throw IllegalArgumentException("AddProductElementView must have a text for its button (buttonText)")
            }
            binding.addElementButton.text = buttonText
        }
    }

    fun initView(callback: (View) -> Unit) {
        binding.addElementButton.setOnClickListener { view -> callback(view) }
    }
}
