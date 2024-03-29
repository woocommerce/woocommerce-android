package com.woocommerce.android.ui.orders

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.extensions.copyToClipboard
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.AIThankYouNoteViewModel.CopyDescriptionToClipboard
import com.woocommerce.android.ui.orders.AIThankYouNoteViewModel.ShareNote
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AIThankYouNoteBottomSheetFragment : WCBottomSheetDialogFragment() {
    private val viewModel: AIThankYouNoteViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    AIThankYouNoteBottomSheet(viewModel)
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
                is CopyDescriptionToClipboard -> copyDescriptionToClipboard(event.description)
                is ShareNote -> {
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, event.note)
                        type = "text/plain"
                    }
                    val title = resources.getText(R.string.product_share_dialog_title)
                    startActivity(Intent.createChooser(shareIntent, title))
                }
            }
        }
    }

    private fun copyDescriptionToClipboard(description: String) {
        context?.copyToClipboard(getString(R.string.ai_order_thank_you_note_copy_label), description)
    }
}
