package com.woocommerce.android.ui.inbox

import android.os.*
import android.view.*
import androidx.compose.ui.platform.*
import androidx.fragment.app.*
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.*
import com.woocommerce.android.ui.compose.theme.*
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxNoteActionEvent.*
import com.woocommerce.android.util.*
import com.woocommerce.android.viewmodel.MultiLiveEvent.*
import dagger.hilt.android.*
import javax.inject.*

@AndroidEntryPoint
class InboxFragment : BaseFragment() {

    private val viewModel: InboxViewModel by viewModels()

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override fun getFragmentTitle() = getString(R.string.inbox_screen_title)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        return ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    InboxScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_inbox, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_dismiss_all -> {
                viewModel.dismissAllNotes()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OpenUrlEvent -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                is Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                else -> event.isHandled = false
            }
        }
    }
}
