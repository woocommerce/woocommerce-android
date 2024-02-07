package com.woocommerce.android.ui.products.adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.woocommerce.android.R
import com.woocommerce.android.databinding.BundledProductItemViewBinding
import com.bumptech.glide.Glide
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.BundledProduct
import com.woocommerce.android.ui.products.ProductStockStatus
import org.wordpress.android.util.PhotonUtils

class BundleProductViewHolder(val viewBinding: BundledProductItemViewBinding) :
    RecyclerView.ViewHolder(viewBinding.root) {
    private val imageSize = itemView.resources.getDimensionPixelSize(R.dimen.image_minor_100)
    private val imageCornerRadius = itemView.resources.getDimensionPixelSize(R.dimen.corner_radius_image)
    fun bind(bundledProduct: BundledProduct) {
        viewBinding.productName.text = bundledProduct.title
        showStockText(bundledProduct.stockStatus)
        showProductImage(bundledProduct.imageUrl)
        showProductSku(bundledProduct.sku)
    }

    private fun showProductImage(imageUrl: String?) {
        val size: Int
        when {
            imageUrl.isNullOrEmpty() -> {
                size = imageSize / 2
                viewBinding.productImage.setImageResource(R.drawable.ic_product)
            }
            else -> {
                size = imageSize
                val photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, imageSize, imageSize)
                Glide.with(viewBinding.productImage)
                    .load(photonUrl)
                    .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
                    .placeholder(R.drawable.ic_product)
                    .into(viewBinding.productImage)
            }
        }
        viewBinding.productImage.layoutParams.apply {
            height = size
            width = size
        }
    }

    private fun showStockText(status: ProductStockStatus) {
        val stockText = when (status) {
            ProductStockStatus.InStock -> {
                itemView.resources.getString(R.string.product_stock_status_instock)
            }
            ProductStockStatus.OutOfStock -> {
                itemView.resources.getString(R.string.product_stock_status_out_of_stock)
            }
            ProductStockStatus.OnBackorder -> {
                itemView.resources.getString(R.string.product_stock_status_on_backorder)
            }
            else -> {
                status.value
            }
        }
        viewBinding.productStock.text = stockText
    }

    private fun showProductSku(sku: String?) {
        if (sku.isNotNullOrEmpty()) {
            viewBinding.productSku.run {
                visibility = View.VISIBLE
                text = itemView.resources.getString(R.string.orderdetail_product_lineitem_sku_value, sku)
            }
        } else {
            viewBinding.productSku.visibility = View.GONE
        }
    }
}
