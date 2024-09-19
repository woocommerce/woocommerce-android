package com.woocommerce.android.ui.products.ai.description

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.extensions.copyToClipboard
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.CopyDescriptionToClipboard
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class AIProductDescriptionBottomSheetFragment : WCBottomSheetDialogFragment() {
    companion object {
        const val KEY_AI_GENERATED_DESCRIPTION_RESULT = "key_ai_generated_description_result"
    }

    private val viewModel: AIProductDescriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Woo_Theme_BottomSheetDialog_Resizeable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    AIProductDescriptionBottomSheet(viewModel = viewModel)
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
                is ExitWithResult<*> -> navigateBackWithResult(KEY_AI_GENERATED_DESCRIPTION_RESULT, event.data)
                is CopyDescriptionToClipboard -> copyDescriptionToClipboard(event.description)
            }
        }
    }

    private fun copyDescriptionToClipboard(description: String) {
        try {
            context?.copyToClipboard(getString(R.string.ai_product_description_label), description)
            ToastUtils.showToast(context, R.string.ai_product_description_copy_success)
        } catch (e: IllegalStateException) {
            WooLog.e(WooLog.T.UTILS, e)
            ToastUtils.showToast(context, R.string.ai_product_description_copy_error)
        }
    }
}
