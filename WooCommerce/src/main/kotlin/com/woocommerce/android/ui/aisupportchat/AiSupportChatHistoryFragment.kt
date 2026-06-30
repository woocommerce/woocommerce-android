package com.woocommerce.android.ui.aisupportchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AiSupportChatHistoryFragment : Fragment() {
    private val viewModel: AiSupportChatHistoryViewModel by viewModels()

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var selectedSite: SelectedSite

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        composeView {
            AiSupportChatHistoryScreen(
                viewModel = viewModel,
                onBookmarkDeleted = viewModel::onDeleteBookmark,
                onBookmarkClicked = { bookmark ->
                    startActivity(
                        AiSupportChatActivity.createResumeIntent(
                            context = requireContext(),
                            chatId = bookmark.chatId,
                            botSlug = bookmark.botSlug,
                            sessionId = bookmark.sessionId,
                            hasCreatedTicket = bookmark.hasCreatedTicket,
                            isResolved = bookmark.isResolved,
                            extraTags = bookmark.extraTags,
                            siteAddress = selectedSite.getIfExists()?.url
                        )
                    )
                }
            )
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadHistory()
    }
}
