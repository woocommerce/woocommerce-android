package com.woocommerce.android.ui.products.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.extensions.copyToClipboard
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class AIProductNameBottomSheetFragment : WCBottomSheetDialogFragment() {
    companion object {
        const val KEY_AI_GENERATED_PRODUCT_NAME_RESULT = "key_ai_generated_product_name_result"
    }

    private val viewModel: AIProductNameViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    AIProductNameBottomSheet(viewModel = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEvents()
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ExitWithResult<*> -> navigateBackWithResult(
                    KEY_AI_GENERATED_PRODUCT_NAME_RESULT,
                    event.data
                )

                is AIProductNameViewModel.CopyProductNameToClipboard -> copyProductNameToClipboard(
                    event.productName
                )
            }
        }
    }

    private fun copyProductNameToClipboard(name: String) {
        try {
            context?.copyToClipboard(getString(R.string.ai_product_name_copy_label), name)
            ToastUtils.showToast(context, R.string.ai_product_name_copy_success)
        } catch (e: IllegalStateException) {
            WooLog.e(WooLog.T.UTILS, e)
            ToastUtils.showToast(context, R.string.ai_product_name_copy_error)
        }
    }
}
