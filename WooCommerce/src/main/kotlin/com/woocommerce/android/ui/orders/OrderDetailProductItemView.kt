package com.woocommerce.android.ui.orders

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.bold
import androidx.core.view.isVisible
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailProductItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

typealias ViewAddonClickListener = (Order.Item) -> Unit

class OrderDetailProductItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    private val binding =
        OrderDetailProductItemBinding.inflate(LayoutInflater.from(ctx), this, true)

    fun initView(
        item: Order.Item,
        productImage: String?,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        onViewAddonsClick: ViewAddonClickListener?,
        addons: List<Addon>,
    ) {
        binding.productInfoName.text = item.name

        val orderTotal = formatCurrencyForDisplay(item.total)
        binding.productInfoTotal.text = orderTotal

        val productPrice = formatCurrencyForDisplay(item.price)
        val attributes = item.attributesDescription
            .takeIf { it.isNotEmpty() }
            ?.let { "$it \u2981 " }
            ?: StringUtils.EMPTY
        binding.productInfoAttributes.text = context.getString(
            R.string.orderdetail_product_lineitem_attributes,
            attributes, item.quantity.formatToString(), productPrice
        )

        if (addons.isNotEmpty()) {
            binding.productInfoAddonsDescription.visibility = VISIBLE
            binding.productInfoAddonsDescription.text = addons.formatToString()
        }

        with(binding.productInfoSKU) {
            isVisible = item.sku.isNotEmpty()
            val productSku =
                context.getString(R.string.orderdetail_product_lineitem_sku_value, item.sku)
            binding.productInfoSKU.text = productSku
        }

        onViewAddonsClick?.let { onClick ->
            binding.productInfoAddons.visibility =
                if (item.containsAddons && AppPrefs.isProductAddonsEnabled) VISIBLE
                else GONE
            binding.productInfoAddons.setOnClickListener { onClick(item) }
        } ?: binding.productInfoAddons.let { it.visibility = GONE }

        productImage?.let {
            val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
            val imageCornerRadius =
                context.resources.getDimensionPixelSize(R.dimen.corner_radius_image)
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
            GlideApp.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_product)
                .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
                .into(binding.productInfoIcon)
        } ?: binding.productInfoIcon.setImageResource(R.drawable.ic_product)
    }

    private fun List<Addon>.formatToString(): SpannableStringBuilder {
        val addonsList = this.groupBy(Addon::name)
            .map { (addonName, addons): Map.Entry<String, List<Addon>> ->
                val spannableAddon = SpannableStringBuilder()

                val allOptionsForGivenAddon = addons.flatMap {
                    if (it is Addon.HasOptions) {
                        it.options
                    } else {
                        emptyList()
                    }
                }

                spannableAddon.append(addonName)

                if (allOptionsForGivenAddon.isEmpty()) {
                    addons.first().description?.let {
                        spannableAddon.bold {
                            append(" $it")
                        }
                    }
                } else {
                    val options = allOptionsForGivenAddon.joinToString(
                        separator = " \u2981 ",
                        transform = { option -> option.label.orEmpty() }
                    )
                    spannableAddon.bold {
                        append(" $options")
                    }
                }
                spannableAddon
            }

        return addonsList.joinTo(SpannableStringBuilder(), separator = "\n")
    }
}
