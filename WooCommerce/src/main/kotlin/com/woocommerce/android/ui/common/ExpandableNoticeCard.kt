package com.woocommerce.android.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.ViewExpandableNoticeCardBinding
import com.woocommerce.android.util.WooAnimUtils

class ExpandableNoticeCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = ViewExpandableNoticeCardBinding.inflate(LayoutInflater.from(ctx), this)

    var isExpanded: Boolean
        set(value) {
            binding.noticeViewMore.isChecked = value
            if (value) {
                WooAnimUtils.fadeIn(binding.noticeMorePanel)
            } else {
                WooAnimUtils.fadeOut(binding.noticeMorePanel)
            }
        }
        get() = binding.noticeViewMore.isChecked

    @Suppress("LongParameterList")
    fun initView(
        title: String,
        message: String,
        mainActionText: String,
        secondaryActionText: String,
        isExpanded: Boolean = false,
        mainActionClick: () -> Unit = {},
        secondaryActionClick: () -> Unit = {}
    ) {
        binding.noticeViewMore.setOnCheckedChangeListener { _, isChecked ->
            this.isExpanded = isChecked
        }

        this.isExpanded = isExpanded

        binding.noticeViewMore.run {
            textOn = title
            textOff = title
            text = title
        }
        binding.noticeMessage.text = message

        binding.btnMainAction.run {
            text = mainActionText
            setOnClickListener { mainActionClick() }
        }
        binding.btnSecondaryAction.run {
            text = secondaryActionText
            setOnClickListener { secondaryActionClick() }
        }
    }
}
