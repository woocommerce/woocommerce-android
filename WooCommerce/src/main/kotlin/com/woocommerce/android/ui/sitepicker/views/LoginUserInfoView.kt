package com.woocommerce.android.ui.sitepicker.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.databinding.ViewLoginUserInfoBinding
import com.woocommerce.android.di.GlideApp

class LoginUserInfoView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = ViewLoginUserInfoBinding.inflate(LayoutInflater.from(ctx), this)

    var displayName: CharSequence?
        get() = binding.textDisplayname.text.toString()
        set(value) {
            binding.textDisplayname.text = value
        }

    var userName
        get() = binding.textUsername.text.toString()
        set(value) {
            binding.textUsername.text = value
        }

    fun centerAlign() {
        binding.loginUserInfo.gravity = Gravity.CENTER
        with(binding.imageAvatar) {
            layoutParams.height = resources.getDimensionPixelSize(dimen.image_major_64)
            layoutParams.width = resources.getDimensionPixelSize(dimen.image_major_64)
            requestLayout()
        }
    }

    fun avatarUrl(avatarUrl: String) {
        GlideApp.with(this)
            .load(avatarUrl)
            .placeholder(drawable.img_gravatar_placeholder)
            .circleCrop()
            .into(binding.imageAvatar)
    }
}
