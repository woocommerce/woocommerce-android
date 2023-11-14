package com.woocommerce.android.ui.feedback

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StringRes
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FeedbackBannerBinding

class FeedbackBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {
    private val binding = FeedbackBannerBinding.inflate(LayoutInflater.from(context), this, true)
    var onSendFeedbackListener: () -> Unit = {}
    var onDismissClickListener: () -> Unit = {}

    init {
        val attributeArray = context.theme.obtainStyledAttributes(attrs, R.styleable.FeedbackBanner, 0, 0)
        binding.title.text = attributeArray.getString(R.styleable.FeedbackBanner_title).orEmpty()
        binding.message.text = attributeArray.getString(R.styleable.FeedbackBanner_message).orEmpty()
        binding.dismissButton.setOnClickListener { onDismissClickListener() }
        binding.sendFeedbackButton.setOnClickListener { onSendFeedbackListener() }
    }

    fun setMessage(@StringRes message: Int) {
        binding.message.setText(message)
    }

    fun setTitle(@StringRes title: Int) {
        binding.title.setText(title)
    }
}
