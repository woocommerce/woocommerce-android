package com.woocommerce.android.ui.orders.creation.products

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationProductDetailsBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.PhotonUtils
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreationProductDetailsFragment : BaseFragment(R.layout.fragment_order_creation_product_details) {
    private val sharedViewModel: OrderCreationViewModel by navGraphViewModels(R.id.nav_graph_order_creations)
    private val navArgs: OrderCreationProductDetailsFragmentArgs by navArgs()

    @Inject lateinit var productImageMap: ProductImageMap
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentOrderCreationProductDetailsBinding.bind(view)
        val item = navArgs.item
        val priceFormatter = currencyFormatter.buildBigDecimalFormatter(
            currencyCode = sharedViewModel.currentDraft.currency
        )
        with(binding) {
            productName.text = item.name

            productAttributes.text = getString(
                R.string.orderdetail_product_lineitem_qty_and_price,
                item.quantity.toString(),
                priceFormatter(item.price)
            )
            loadImage(item)

            removeProductButton.setOnClickListener {
                sharedViewModel.onRemoveProduct(item)
                findNavController().navigateUp()
            }
        }
    }

    override fun getFragmentTitle(): String = getString(R.string.order_creation_product_details_title)

    private fun FragmentOrderCreationProductDetailsBinding.loadImage(item: Order.Item) {
        val imageSize = resources.getDimensionPixelSize(R.dimen.image_major_50)
        PhotonUtils.getPhotonImageUrl(
            productImageMap.get(item.uniqueId), imageSize, imageSize
        )?.let { imageUrl ->
            GlideApp.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_product)
                .into(productIcon)
        }
    }
}
