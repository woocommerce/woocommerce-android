package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.FeatureWipNoticeBinding
import com.woocommerce.android.util.WooAnimUtils

class FeatureWIPNoticeCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = FeatureWipNoticeBinding.inflate(LayoutInflater.from(ctx), this)

    private var isExpanded: Boolean
        set(value) {
            binding.featureWipViewMore.isChecked = value
            if (value) {
                WooAnimUtils.fadeIn(binding.featureWipMorePanel)
            } else {
                WooAnimUtils.fadeOut(binding.featureWipMorePanel)
            }
        }
        get() = binding.featureWipViewMore.isChecked

    fun initView(
        title: String,
        message: String,
        onGiveFeedbackClick: () -> Unit = {},
        onDismissClick: () -> Unit = {}
    ) {
        binding.featureWipViewMore.setOnCheckedChangeListener { _, isChecked ->
            isExpanded = isChecked
        }

        isExpanded = false

        binding.featureWipViewMore.textOn = title
        binding.featureWipViewMore.textOff = title
        binding.featureWipViewMore.text = title
        binding.featureWipMessage.text = message

        binding.btnGiveFeedback.setOnClickListener { onGiveFeedbackClick() }
        binding.btnDismiss.setOnClickListener { onDismissClick() }
    }
}
