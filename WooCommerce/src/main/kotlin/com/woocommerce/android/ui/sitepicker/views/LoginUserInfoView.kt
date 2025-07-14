package com.woocommerce.android.ui.sitepicker.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ViewLoginUserInfoBinding

class LoginUserInfoView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(ctx, attrs, defStyleAttr) {
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

    init {
        background = ResourcesCompat.getDrawable(resources, R.drawable.bg_rounded_box, context.theme)
    }

    fun centerAlign() {
        binding.loginUserInfo.gravity = Gravity.CENTER
        with(binding.imageAvatar) {
            layoutParams.height = resources.getDimensionPixelSize(R.dimen.image_major_64)
            layoutParams.width = resources.getDimensionPixelSize(R.dimen.image_major_64)
            requestLayout()
        }
    }

    fun avatarUrl(avatarUrl: String) {
        Glide.with(this)
            .load(avatarUrl)
            .placeholder(R.drawable.img_gravatar_placeholder)
            .circleCrop()
            .into(binding.imageAvatar)
    }
}
