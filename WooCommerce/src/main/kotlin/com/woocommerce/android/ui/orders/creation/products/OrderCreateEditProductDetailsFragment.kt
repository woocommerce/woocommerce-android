package com.woocommerce.android.ui.orders.creation.products

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditProductDetailsBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreateEditProductDetailsFragment : BaseFragment(R.layout.fragment_order_create_edit_product_details) {
    private val sharedViewModel: OrderCreateEditViewModel by hiltNavGraphViewModels(R.id.nav_graph_order_creations)
    private val navArgs: OrderCreateEditProductDetailsFragmentArgs by navArgs()

    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = R.drawable.ic_gridicons_cross_24dp
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentOrderCreateEditProductDetailsBinding.bind(view)
        val item = navArgs.item
        val uiModel = sharedViewModel.getProductUIModelFromItem(item)

        with(binding) {
            productItemView.bind(
                uiModel,
                currencyFormatter,
                sharedViewModel.currentDraft.currency
            )

            removeProductButton.setOnClickListener {
                sharedViewModel.onRemoveProduct(item)
                findNavController().navigateUp()
            }
        }
    }

    override fun getFragmentTitle(): String = getString(R.string.order_creation_product_details_title)
}
