package com.woocommerce.android.ui.aisupportchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.compose.composeView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AiSupportChatHistoryFragment : Fragment() {
    private val viewModel: AiSupportChatHistoryViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        composeView {
            AiSupportChatHistoryScreen(
                viewModel = viewModel,
                onBookmarkClicked = { bookmark ->
                    startActivity(
                        AiSupportChatActivity.createResumeIntent(
                            context = requireContext(),
                            chatId = bookmark.chatId,
                            botSlug = bookmark.botSlug,
                            sessionId = bookmark.sessionId,
                            hasCreatedTicket = bookmark.hasCreatedTicket,
                            isResolved = bookmark.isResolved
                        )
                    )
                }
            )
        }

    override fun onResume() {
        super.onResume()
        viewModel.loadHistory()
    }
}
