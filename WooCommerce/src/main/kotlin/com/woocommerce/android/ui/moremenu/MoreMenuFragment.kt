package com.woocommerce.android.ui.moremenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentMoreMenuBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@ExperimentalFoundationApi
class MoreMenuFragment : TopLevelFragment(R.layout.fragment_more_menu) {
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var appPrefs: AppPrefsWrapper

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
        val wpAdminUrl = selectedSite.get().adminUrl
        val storeUrl = selectedSite.get().url

        val buttons = listOf(
            MenuButton(
                R.string.more_menu_button_woo_admin,
                R.drawable.ic_more_menu_wp_admin
            ) {
                ChromeCustomTabUtils.launchUrl(requireContext(), wpAdminUrl)
            },
            MenuButton(
                R.string.more_menu_button_store,
                R.drawable.ic_more_menu_store
            ) {
                ChromeCustomTabUtils.launchUrl(requireContext(), storeUrl)
            },
            MenuButton(
                R.string.more_menu_button_reviews,
                R.drawable.ic_more_menu_reviews,
                badgeCount = if (appPrefs.hasUnseenReviews()) 1 else 0
            ) {
                findNavController().navigateSafely(
                    MoreMenuFragmentDirections.actionMoreMenuToReviewList()
                )
            }
            // MenuButton(R.string.more_menu_button_analytics, R.drawable.ic_more_menu_analytics),
            // MenuButton(R.string.more_menu_button_payments, R.drawable.ic_more_menu_payments),
            // MenuButton(R.string.more_menu_button_inbox, R.drawable.ic_more_menu_inbox),
        )

        binding.menu.apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                // In Compose world
                MaterialTheme {
                    MoreMenu(buttons) {
                        findNavController().navigateSafely(
                            MoreMenuFragmentDirections.actionMoreMenuToSettingsActivity()
                        )
                    }
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
