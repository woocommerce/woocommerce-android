package com.woocommerce.android.ui.dashboard.topcategories.categoryproducts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.details.ProductDetailFragment.Mode.ShowProduct
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryProductsFragment : BaseFragment() {
    private val viewModel: CategoryProductsViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(hasShadow = false)

    override fun getFragmentTitle() =
        viewModel.state.value?.categoryName ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                WooThemeWithBackground {
                    val state = viewModel.state.observeAsState()
                    state.value?.let {
                        CategoryProductsScreen(
                            state = it,
                            onRetryClicked = viewModel::onRetryClicked
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is CategoryProductsViewModel.OpenProduct -> {
                    findNavController().navigateSafely(
                        NavGraphMainDirections.actionGlobalProductDetailFragment(
                            mode = ShowProduct(event.productId),
                        )
                    )
                }
                is MultiLiveEvent.Event -> {}
            }
        }
    }
}
