package com.woocommerce.android.ui.payments.feedback.ipp

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StringRes
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.IppFeedbackBannerBinding

class FeedbackBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {
    private val binding = IppFeedbackBannerBinding.inflate(LayoutInflater.from(context), this, true)

    var onCTAClickListener: () -> Unit = {}

    var onDismissClickListener: () -> Unit = {}

    init {
        binding.dismissButton.setOnClickListener {
            onDismissClickListener()
        }

        binding.ctaButton.setOnClickListener {
            onCTAClickListener()
        }
    }

    fun setMessage(@StringRes message: Int) {
        binding.message.setText(message)
    }

    fun setTitle(@StringRes title: Int) {
        binding.title.setText(title)
    }
}
