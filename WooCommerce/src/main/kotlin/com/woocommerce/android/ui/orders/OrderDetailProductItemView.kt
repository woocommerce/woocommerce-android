package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailProductItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

class OrderDetailProductItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailProductItemBinding.inflate(LayoutInflater.from(ctx), this, true)

    fun initView(
        item: Order.Item,
        productImage: String?,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        binding.productInfoName.text = item.name

        val orderTotal = formatCurrencyForDisplay(item.total)
        binding.productInfoTotal.text = orderTotal

        val productPrice = formatCurrencyForDisplay(item.price)
        val attributes = item.attributesList.takeIf { it.isNotEmpty() }?.let { "$it \u2981 " } ?: StringUtils.EMPTY
        binding.productInfoAttributes.text = context.getString(
            R.string.orderdetail_product_lineitem_attributes,
            attributes, item.quantity.toString(), productPrice
        )

        val productSku = context.getString(R.string.orderdetail_product_lineitem_sku_value, item.sku)
        binding.productInfoSKU.text = productSku

        productImage?.let {
            val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
            GlideApp.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(binding.productInfoIcon)
        } ?: binding.productInfoIcon.setImageResource(R.drawable.ic_product)
    }
}
