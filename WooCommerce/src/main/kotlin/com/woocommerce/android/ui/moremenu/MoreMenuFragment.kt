package com.woocommerce.android.ui.moremenu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentMoreMenuBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.moremenu.MenuButtonType.*
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.*
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@ExperimentalFoundationApi
class MoreMenuFragment : TopLevelFragment(R.layout.fragment_more_menu) {
    @Inject lateinit var selectedSite: SelectedSite

    override fun getFragmentTitle() = getString(R.string.more_menu)

    override fun shouldExpandToolbar(): Boolean = false

    private var _binding: FragmentMoreMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoreMenuViewModel by viewModels()

    private val requestSitePicker = registerForActivityResult(StartActivityForResult()) {
        viewModel.handleStoreSwitch()
    }

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
            MenuButton(VIEW_ADMIN, R.string.more_menu_button_woo_admin, R.drawable.ic_more_menu_wp_admin),
            MenuButton(VIEW_STORE, R.string.more_menu_button_store, R.drawable.ic_more_menu_store),
            // MenuButton(R.string.more_menu_button_analytics, R.drawable.ic_more_menu_analytics),
            // MenuButton(R.string.more_menu_button_payments, R.drawable.ic_more_menu_payments),
            // MenuButton(R.string.more_menu_button_inbox, R.drawable.ic_more_menu_inbox),
            MenuButton(REVIEWS, R.string.more_menu_button_reviews, R.drawable.ic_more_menu_reviews)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(this) { event ->
            when (event) {
                is NavigateToSettingsEvent -> navigateToSettings()
                is StartSitePickerEvent -> startSitePicker()
                is ViewAdminEvent -> openInBrowser(event.url)
                is ViewStoreEvent -> openInBrowser(event.url)
                is ViewReviewsEvent -> navigateToReviews()
            }
        }
    }

    private fun navigateToSettings() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToSettingsActivity()
        )
    }

    private fun startSitePicker() {
        val sitePickerIntent = Intent(context, SitePickerActivity::class.java)
        requestSitePicker.launch(sitePickerIntent)
    }

    private fun openInBrowser(url: String) {
        ChromeCustomTabUtils.launchUrl(requireContext(), url)
    }

    private fun navigateToReviews() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToReviewList()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
