package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.woocommerce.android.databinding.ProductSearchViewLayoutBinding
import com.woocommerce.android.util.WooAnimUtils

/**
 * Used by product list to choose whether to search product details or product SKU
 */
class WCProductSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private var binding = ProductSearchViewLayoutBinding.inflate(LayoutInflater.from(context))
    private var listener: ProductSearchTypeChangedListener? = null

    var productSearchType = ProductSearchType.SEARCH_ALL
        get() = field
        set(value) {
            if (value != field) {
                field = value
                binding.textSearchAll.isSelected = value == ProductSearchType.SEARCH_ALL
                binding.textSearchSku.isSelected = value == ProductSearchType.SEARCH_SKU
                listener?.onProductSearchTypeChanged(productSearchType)
            }
        }

    init {
        binding.textSearchAll.setOnClickListener {
            productSearchType = ProductSearchType.SEARCH_ALL
        }
        binding.textSearchSku.setOnClickListener {
            productSearchType = ProductSearchType.SEARCH_SKU
        }
    }

    fun show(searchTypeListener: ProductSearchTypeChangedListener? = null) {
        listener = searchTypeListener
        if (!isVisible) {
            WooAnimUtils.scaleIn(this)
        }
    }

    fun hide() {
        listener = null
        if (isVisible) {
            WooAnimUtils.scaleOut(this)
        }
    }

    enum class ProductSearchType {
        SEARCH_ALL,
        SEARCH_SKU
    }

    interface ProductSearchTypeChangedListener {
        fun onProductSearchTypeChanged(searchType: ProductSearchType)
    }
}
