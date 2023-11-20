package com.woocommerce.android.ui.orders.creation.product.discount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreateEditProductDiscountFragment : BaseFragment() {
    private val viewModel: OrderCreateEditProductDiscountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    OrderCreateEditProductDiscountScreen(
                        viewState = viewModel.viewState,
                        onCloseClicked = viewModel::onNavigateBack,
                        onDoneClicked = viewModel::onDoneClicked,
                        onRemoveDiscountClicked = viewModel::onDiscountRemoveClicked,
                        onDiscountAmountChange = viewModel::onDiscountAmountChange,
                        onAmountDiscountSelected = viewModel::onAmountDiscountSelected,
                        onPercentageDiscountSelected = viewModel::onPercentageDiscountSelected,
                        discountInputFieldConfig = viewModel.discountInputFieldConfig,
                        productItem = viewModel.productItem,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is ExitWithResult<*> -> {
                    navigateBackWithResult(
                        KEY_PRODUCT_DISCOUNT_RESULT,
                        it.data,
                        R.id.orderCreationFragment
                    )
                }

                is Exit -> {
                    findNavController().popBackStack()
                }
            }
        }
    }

    override val activityAppBarStatus: AppBarStatus = AppBarStatus.Hidden

    companion object {
        const val KEY_PRODUCT_DISCOUNT_RESULT = "key_product_discount_result"
    }
}
