package com.woocommerce.android.ui.products.ai

import android.net.Uri
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
import com.woocommerce.android.mediapicker.MediaPickerHelper
import com.woocommerce.android.mediapicker.MediaPickerHelper.MediaPickerResultHandler
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductPricing
import com.woocommerce.android.ui.products.ProductNavigator
import com.woocommerce.android.ui.products.ai.AddProductWithAIViewModel.EditPrice
import com.woocommerce.android.ui.products.ai.AddProductWithAIViewModel.NavigateToProductDetailScreen
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.PackagePhotoData
import com.woocommerce.android.ui.products.ai.ProductNameSubViewModel.NavigateToAIProductNameBottomSheet
import com.woocommerce.android.ui.products.ai.ProductNameSubViewModel.ShowMediaLibrary
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import com.woocommerce.android.ui.products.price.ProductPricingViewModel.PricingData
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddProductWithAIFragment : BaseFragment(), MediaPickerResultHandler {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: AddProductWithAIViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var mediaPickerHelper: MediaPickerHelper

    @Inject lateinit var navigator: ProductNavigator

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
                        mode = ProductDetailFragment.Mode.ShowProduct(
                            remoteProductId = event.productId,
                            afterGeneratedWithAi = true,
                        )
                    ),
                    navOptions = navOptions {
                        popUpTo(R.id.addProductWithAIFragment) { inclusive = true }
                    }
                )

                is EditPrice -> navigator.navigate(
                    fragment = this,
                    ViewProductPricing(
                       PricingData(
                           regularPrice = event.suggestedPrice,
                       )
                    )
                )

                is ShowMediaLibrary -> mediaPickerHelper.showMediaPicker(event.source)

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

        handleDialogResult<PackagePhotoData>(
            key = PackagePhotoBottomSheetFragment.KEY_PACKAGE_PHOTO_SCAN_RESULT,
            entryId = R.id.addProductWithAIFragment
        ) { data ->
            viewModel.onProductPackageScanned(data.title, data.description, data.keywords)
        }
    }

    private fun navigateToAIProductName(initialName: String? = null) {
        val action =
            AddProductWithAIFragmentDirections
                .actionAddProductWithAIFragmentToAIProductNameBottomSheetFragment(initialName)
        findNavController().navigateSafely(action)
    }

    override fun onDeviceMediaSelected(imageUris: List<Uri>, source: String) {
        if (imageUris.isNotEmpty()) {
            onImageSelected(imageUris.first().toString())
        }
    }

    override fun onWPMediaSelected(images: List<Image>) {
        if (images.isNotEmpty()) {
            onImageSelected(images.first().source)
        }
    }

    private fun onImageSelected(mediaUri: String) {
        findNavController().navigateSafely(
            directions = AddProductWithAIFragmentDirections
                .actionAddProductWithAIFragmentToPackagePhotoBottomSheetFragment(mediaUri)
        )
    }
}
