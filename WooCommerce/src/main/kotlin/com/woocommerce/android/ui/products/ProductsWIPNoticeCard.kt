package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.util.WooAnimUtils
import kotlinx.android.synthetic.main.products_wip_notice.view.*

class ProductsWIPNoticeCard @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.products_wip_notice, this)
    }

    var isExpanded: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                if (value) {
                    WooAnimUtils.fadeIn(products_wip_morePanel)
                } else {
                    WooAnimUtils.fadeOut(products_wip_morePanel)
                }
            }
        }

    fun initView() {
        products_wip_viewMore.setOnCheckedChangeListener { _, isChecked ->
            isExpanded = isChecked
        }
    }
}
