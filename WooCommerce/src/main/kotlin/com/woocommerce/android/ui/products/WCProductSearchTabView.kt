package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductSearchViewLayoutBinding
import com.woocommerce.android.util.WooAnimUtils

/**
 * Used by product list to choose whether to search product details or product SKU
 */
class WCProductSearchTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), TabLayout.OnTabSelectedListener {
    private var binding = ProductSearchViewLayoutBinding.inflate(LayoutInflater.from(context), this, true)
    private var listener: ProductSearchTypeChangedListener? = null

    init {
        binding.tabLayout.addTab(
            binding.tabLayout.newTab().apply {
                setText(context.getString(R.string.product_search_all))
                    .id = TAB_ALL
            }
        )
        binding.tabLayout.addTab(
            binding.tabLayout.newTab().apply {
                setText(context.getString(R.string.product_search_sku))
                    .id = TAB_SKU
            }
        )

        binding.tabLayout.addOnTabSelectedListener(this)
    }

    fun show(
        searchTypeListener: ProductSearchTypeChangedListener? = null,
        isSkuSearch: Boolean = false
    ) {
        if (isSkuSearch) {
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(TAB_SKU))
        }

        listener = searchTypeListener
        if (!isVisible) {
            WooAnimUtils.fadeIn(this)
        }
    }

    fun hide() {
        listener = null
        if (isVisible) {
            WooAnimUtils.fadeOut(this)
        }
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        tab?.let {
            listener?.onProductSearchTypeChanged(
                isSkuSearch = binding.tabLayout.selectedTabPosition == TAB_SKU
            )
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        // noop
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        // noop
    }

    interface ProductSearchTypeChangedListener {
        fun onProductSearchTypeChanged(isSkuSearch: Boolean)
    }

    companion object {
        private const val TAB_ALL = 0
        private const val TAB_SKU = 1
    }
}
