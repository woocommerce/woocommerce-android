package com.woocommerce.android.ui.products.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.ai.AddProductWithAIViewModel.NavigateToProductDetailScreen
import com.woocommerce.android.ui.products.ai.ProductNameSubViewModel.NavigateToAIProductNameBottomSheet
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddProductWithAIFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: AddProductWithAIViewModel by viewModels()
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    AddProductWithAIScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleEvents()
        handleResults()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToAIProductNameBottomSheet -> navigateToAIProductName(event.initialName)
                is NavigateToProductDetailScreen -> findNavController().navigateSafely(
                    directions = NavGraphMainDirections.actionGlobalProductDetailFragment(
                        remoteProductId = event.productId,
                        isAIContent = true
                    ),
                    navOptions = navOptions {
                        popUpTo(R.id.addProductWithAIFragment) { inclusive = true }
                    }
                )

                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)

                Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun handleResults() {
        handleDialogResult<String>(
            key = AIProductNameBottomSheetFragment.KEY_AI_GENERATED_PRODUCT_NAME_RESULT,
            entryId = R.id.addProductWithAIFragment
        ) { productName ->
            viewModel.onProductNameGenerated(productName)
        }
    }

    private fun navigateToAIProductName(initialName: String? = null) {
        val action =
            AddProductWithAIFragmentDirections
                .actionAddProductWithAIFragmentToAIProductNameBottomSheetFragment(initialName)
        findNavController().navigateSafely(action)
    }
}
