package com.woocommerce.android.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentInboxBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxNoteActionEvent
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InboxFragment : BaseFragment(R.layout.fragment_inbox) {
    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InboxViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)

        val view = binding.root
        binding.inboxComposeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    Inbox(viewModel = viewModel)
                }
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            if (event is InboxNoteActionEvent.OpenUrlEvent) {
                ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
            }
        }
    }
}
