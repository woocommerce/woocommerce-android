package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.util.WooAnimUtils
import kotlinx.android.synthetic.main.feature_wip_notice.view.*

class FeatureWIPNoticeCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.feature_wip_notice, this)
    }

    private var isExpanded: Boolean
        set(value) {
            feature_wip_viewMore.isChecked = value
            if (value) {
                WooAnimUtils.fadeIn(feature_wip_morePanel)
            } else {
                WooAnimUtils.fadeOut(feature_wip_morePanel)
            }
        }
        get() = feature_wip_viewMore.isChecked

    fun initView(
        title: String,
        message: String,
        onGiveFeedbackClick: (View) -> Unit = {},
        onDismissClick: (View) -> Unit = {}
    ) {
        feature_wip_viewMore.setOnCheckedChangeListener { _, isChecked ->
            isExpanded = isChecked
        }

        isExpanded = false
        feature_wip_viewMore.textOn = title
        feature_wip_viewMore.textOff = title
        feature_wip_viewMore.text = title
        feature_wip_message.text = message

        btn_give_feedback.setOnClickListener { onGiveFeedbackClick(it) }
        btn_dismiss.setOnClickListener { onDismissClick(it) }
    }
}
