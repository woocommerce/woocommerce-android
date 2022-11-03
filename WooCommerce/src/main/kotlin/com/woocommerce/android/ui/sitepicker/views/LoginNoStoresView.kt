package com.woocommerce.android.ui.sitepicker.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.woocommerce.android.databinding.ViewLoginNoStoresBinding

class LoginNoStoresView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(ctx, attrs, defStyleAttr) {
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

    var isNoStoresBtnVisible by binding.btnSecondaryAction::isVisible

    fun clickSecondaryAction(onClickListener: ((view: View) -> Unit)) {
        binding.btnSecondaryAction.setOnClickListener(onClickListener)
    }
}
