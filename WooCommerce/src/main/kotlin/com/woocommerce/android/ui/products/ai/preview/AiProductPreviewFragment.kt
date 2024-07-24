package com.woocommerce.android.ui.products.ai.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AiProductPreviewFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: AiProductPreviewViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private var undoSnackBar: Snackbar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            AiProductPreviewScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleEvents()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
                is MultiLiveEvent.Event.ShowUndoSnackbar -> {
                    undoSnackBar = uiMessageResolver.getUndoSnack(
                        message = event.message,
                        actionListener = event.undoAction
                    ).also {
                        it.addCallback(event.dismissAction)
                        it.show()
                    }
                }
                is AiProductPreviewViewModel.NavigateToProductDetailScreen -> findNavController().navigateSafely(
                    directions = NavGraphMainDirections.actionGlobalProductDetailFragment(
                        mode = ProductDetailFragment.Mode.ShowProduct(
                            remoteProductId = event.productId,
                            afterGeneratedWithAi = true,
                        )
                    ),
                    navOptions = navOptions {
                        popUpTo(R.id.AiProductPromptFragment) { inclusive = true }
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        undoSnackBar?.dismiss()
        super.onDestroyView()
    }
}
