package com.woocommerce.android.ui.moremenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentMoreMenuBinding
import com.woocommerce.android.ui.base.TopLevelFragment

@ExperimentalFoundationApi
class MoreMenuFragment : TopLevelFragment(R.layout.fragment_more_menu) {
    override fun getFragmentTitle() = getString(R.string.more_menu)

    override fun shouldExpandToolbar(): Boolean = false

    private var _binding: FragmentMoreMenuBinding? = null
    private val binding get() = _binding!!

    override fun scrollToTop() {
        return
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreMenuBinding.inflate(inflater, container, false)
        val view = binding.root

        // TODO: This is temporary, it will be moved to a ViewModel
        val buttons = listOf(
            MenuButton("WooCommerce Admin", R.drawable.ic_more_menu_wp_admin),
            MenuButton("View Store", R.drawable.ic_more_menu_store),
            MenuButton("Analytics", R.drawable.ic_more_menu_analytics),
            MenuButton("Payments", R.drawable.ic_more_menu_payments),
            MenuButton("Inbox", R.drawable.ic_more_menu_inbox)
        )

        binding.menu.apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                // In Compose world
                MaterialTheme {
                    MoreMenu(buttons)
                }
            }
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
