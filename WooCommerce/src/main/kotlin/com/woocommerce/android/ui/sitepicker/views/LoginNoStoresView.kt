package com.woocommerce.android.ui.sitepicker.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.ViewLoginNoStoresBinding

class LoginNoStoresView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = ViewLoginNoStoresBinding.inflate(LayoutInflater.from(ctx), this)

    var noStoresText: String
        get() = binding.noStoresViewText.text.toString()
        set(value) {
            binding.noStoresViewText.text = value
        }

    var noStoresBtnText: String
        get() = binding.btnSecondaryAction.text.toString()
        set(value) {
            binding.btnSecondaryAction.text = value
        }

    fun clickSecondaryAction(onClickListener: ((view: View) -> Unit)) {
        binding.btnSecondaryAction.setOnClickListener(onClickListener)
    }
}
