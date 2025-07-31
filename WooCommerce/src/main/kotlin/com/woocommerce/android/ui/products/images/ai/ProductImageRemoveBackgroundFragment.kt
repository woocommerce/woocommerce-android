package com.woocommerce.android.ui.products.images.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductImageRemoveBackgroundFragment : BaseFragment() {

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: ProductImageRemoveBackgroundViewModel by viewModels()


    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    ProductImageRemoveBackgroundScreen(
                        state = viewModel.state.collectAsState(),
                        onBackPressed = { handleBackPressed() },
                        onCancelClicked = { findNavController().navigateUp() },
                        onSaveClicked = { handleSaveClicked() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ShowSnackbar -> {
                    uiMessageResolver.showSnack(event.message)
                }
                is MultiLiveEvent.Event.Exit -> {
                    findNavController().popBackStack(R.id.productImagesFragment, false)
                }
            }
        }
    }


    private fun handleBackPressed() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.discard_changes_question)
            .setMessage(R.string.changes_not_saved_message)
            .setPositiveButton(R.string.discard) { _, _ ->
                findNavController().navigateUp()
            }
            .setNegativeButton(R.string.keep_editing) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun handleSaveClicked() {
        val currentState = viewModel.state.value
        if (currentState is ViewState.Success) {
            viewModel.onSaveImageTapped()
        }
    }
}
