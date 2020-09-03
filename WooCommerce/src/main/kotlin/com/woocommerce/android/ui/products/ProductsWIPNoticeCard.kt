package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.util.WooAnimUtils
import kotlinx.android.synthetic.main.products_wip_notice.view.*

class ProductsWIPNoticeCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.products_wip_notice, this)
    }

    private var isExpanded: Boolean
        set(value) {
            products_wip_viewMore.isChecked = value
            if (value) {
                WooAnimUtils.fadeIn(products_wip_morePanel)
            } else {
            WooAnimUtils.fadeOut(products_wip_morePanel)
            }
        }
        get() = products_wip_viewMore.isChecked

    fun initView(
        title: String,
        message: String,
        onGiveFeedbackClick: (View) -> Unit = {},
        onDismissClick: (View) -> Unit = {}
    ) {
        products_wip_viewMore.setOnCheckedChangeListener { _, isChecked ->
            isExpanded = isChecked
        }

        isExpanded = false
        products_wip_viewMore.textOn = title
        products_wip_viewMore.textOff = title
        products_wip_viewMore.text = title
        products_wip_message.text = message

        btn_give_feedback.setOnClickListener(onGiveFeedbackClick)
        btn_dismiss.setOnClickListener(onDismissClick)
    }
}
