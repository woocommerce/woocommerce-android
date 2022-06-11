package com.woocommerce.android.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.widget.ImageViewCompat
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ViewExpandableMessageBinding

class ExpandableMessageView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = ViewExpandableMessageBinding.inflate(LayoutInflater.from(ctx), this)

    var title: String = ""
        set(value) {
            field = value
            binding.title.text = value
        }

    var message: String = ""
        set(value) {
            field = value
            binding.message.text = value
        }

    @DrawableRes
    var icon: Int = 0
        set(value) {
            field = value
            if (value != 0) {
                val drawable = ContextCompat.getDrawable(context, value)
                binding.icon.setImageDrawable(drawable)
            }
        }

    @ColorRes
    var iconTint: Int = 0
        set(value) {
            field = value
            if (value != 0) {
                ImageViewCompat.setImageTintList(
                    binding.icon,
                    AppCompatResources.getColorStateList(context, value)
                )
            }
        }

    var isExpanded: Boolean = false
        set(value) {
            field = value
            if (isExpanded) {
                binding.expandableMessageRoot.transitionToEnd()
            } else {
                binding.expandableMessageRoot.transitionToStart()
            }
        }

    init {
        binding.root.setOnClickListener { isExpanded = !isExpanded }
        context.obtainStyledAttributes(attrs, R.styleable.ExpandableMessageView, 0, 0).use { typedArray ->
            title = typedArray.getString(R.styleable.ExpandableMessageView_title).orEmpty()
            message = typedArray.getString(R.styleable.ExpandableMessageView_message).orEmpty()
            icon = typedArray.getResourceId(R.styleable.ExpandableMessageView_icon, 0)
            iconTint = typedArray.getResourceId(R.styleable.ExpandableMessageView_iconTint, 0)
            isExpanded = typedArray.getBoolean(R.styleable.ExpandableMessageView_isExpanded, false)
        }
    }
}
