package com.woocommerce.android.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.inbox.InboxNoteActionEvent.OpenUrlEvent
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.ChromeCustomTabUtils.Height.Partial.ThreeQuarters
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InboxFragment : BaseFragment(), MenuProvider {
    private val viewModel: InboxViewModel by viewModels()

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override fun getFragmentTitle() = getString(R.string.inbox_screen_title)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
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

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_inbox, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_dismiss_all -> {
                viewModel.dismissAllNotes()
                true
            }
            else -> false
        }
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OpenUrlEvent -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url, ThreeQuarters)
                is Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                else -> event.isHandled = false
            }
        }
    }
}
