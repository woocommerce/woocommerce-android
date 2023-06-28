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

    fun initView(
        title: String,
        message: String,
        onContactSupportClick: () -> Unit = {},
        onTroubleShootingClick: () -> Unit = {}
    ) {
        binding.noticeViewMore.setOnCheckedChangeListener { _, isChecked ->
            isExpanded = isChecked
        }

        isExpanded = false

        binding.noticeViewMore.textOn = title
        binding.noticeViewMore.textOff = title
        binding.noticeViewMore.text = title
        binding.noticeMessage.text = message


        binding.btnTroubleshooting.setOnClickListener { onTroubleShootingClick() }
        binding.btnSupport.setOnClickListener { onContactSupportClick() }
    }
}
