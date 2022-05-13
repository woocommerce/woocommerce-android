package com.woocommerce.android.ui.orders.creation.products

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationProductDetailsBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreationProductDetailsFragment : BaseFragment(R.layout.fragment_order_creation_product_details) {
    private val sharedViewModel: OrderCreationViewModel by hiltNavGraphViewModels(R.id.nav_graph_order_creations)
    private val navArgs: OrderCreationProductDetailsFragmentArgs by navArgs()

    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = R.drawable.ic_gridicons_cross_24dp
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentOrderCreationProductDetailsBinding.bind(view)
        val item = navArgs.item
        val product = sharedViewModel.getProductFromOrderItem(item)

        if (product == null) {
            uiMessageResolver.showSnack(R.string.product_detail_load_product_error)
            findNavController().navigateUp()
            return
        }

        with(binding) {
            productItemView.bind(
                product,
                currencyFormatter
            )
            // TODO nbradbury take sharedViewModel.currentDraft.currency into account

            removeProductButton.setOnClickListener {
                sharedViewModel.onRemoveProduct(item)
                findNavController().navigateUp()
            }
        }
    }

    override fun getFragmentTitle(): String = getString(R.string.order_creation_product_details_title)
}
