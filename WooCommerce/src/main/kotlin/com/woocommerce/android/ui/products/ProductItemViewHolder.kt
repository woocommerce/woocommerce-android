package com.woocommerce.android.ui.products

import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductListItemBinding
import com.woocommerce.android.model.Product
import com.woocommerce.android.util.CurrencyFormatter

class ProductItemViewHolder(val viewBinding: ProductListItemBinding) :
    RecyclerView.ViewHolder(viewBinding.root) {
    private val context = viewBinding.root.context

    fun bind(
        product: Product,
        currencyFormatter: CurrencyFormatter,
        isActivated: Boolean = false
    ) {
        viewBinding.root.isActivated = isActivated
        viewBinding.productItemView.bind(
            product = product,
            currencyFormatter = currencyFormatter,
            isActivated = isActivated
        )

        ViewCompat.setTransitionName(
            viewBinding.root,
            String.format(
                context.getString(R.string.order_card_transition_name),
                product.remoteId
            )
        )
    }

    fun setOnDeleteClickListener(
        product: Product,
        onItemDeleted: (product: Product) -> Unit
    ) {
        with(viewBinding.productItemView.binding.productBtnDelete) {
            isVisible = true
            setOnClickListener { onItemDeleted.invoke(product) }
        }
    }
}
