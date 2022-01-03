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

        val buttons = listOf(
            MenuButton(R.string.more_menu_button_woo_admin, R.drawable.ic_more_menu_wp_admin),
            MenuButton(R.string.more_menu_button_store, R.drawable.ic_more_menu_store),
            MenuButton(R.string.more_menu_button_analytics, R.drawable.ic_more_menu_analytics),
            MenuButton(R.string.more_menu_button_payments, R.drawable.ic_more_menu_payments),
            MenuButton(R.string.more_menu_button_inbox, R.drawable.ic_more_menu_inbox),
            MenuButton(R.string.more_menu_button_reviews, R.drawable.ic_more_menu_reviews)
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
