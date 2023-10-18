package com.woocommerce.android.ui.products.ai

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
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
import com.woocommerce.android.ui.products.ai.ProductNameSubViewModel.ShowMediaLibraryDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.mediapicker.MediaPickerConstants
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.mediapicker.ui.MediaPickerActivity
import javax.inject.Inject

@AndroidEntryPoint
class AddProductWithAIFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: AddProductWithAIViewModel by viewModels()
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var mediaPickerSetupFactory: MediaPickerSetup.Factory

    private val resultLauncher = registerForActivityResult(StartActivityForResult()) {
        handleMediaPickerResult(it)
    }

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
                is ShowMediaLibraryDialog -> viewModel.onMediaLibraryDialogRequested()

                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)

                Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun showPackagePhotoBottomSheet(imageUrl: String) {
        findNavController().navigateSafely(
            directions = AddProductWithAIFragmentDirections
                .actionAddProductWithAIFragmentToPackagePhotoBottomSheetFragment(imageUrl)
        )
    }

    private fun showStorageChooser() {
        val mediaPickerIntent = MediaPickerActivity.buildIntent(
            context = requireContext(),
            mediaPickerSetupFactory.build(DEVICE)
        )
        resultLauncher.launch(mediaPickerIntent)
    }

    private fun handleMediaPickerResult(result: ActivityResult) {
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            result.data?.extras?.let { extra ->
                val uri = (extra.getStringArray(MediaPickerConstants.EXTRA_MEDIA_URIS))
                    ?.map { Uri.parse(it) }
                    ?.first()

                if (uri != null) {
                    showPackagePhotoBottomSheet(uri.toString())
                }
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
