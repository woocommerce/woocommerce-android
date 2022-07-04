package com.woocommerce.android.ui.moremenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.NavigateToSettingsEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.StartSitePickerEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.ViewAdminEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.ViewCouponsEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.ViewInboxEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.ViewReviewsEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.ViewStoreEvent
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@ExperimentalFoundationApi
class MoreMenuFragment : TopLevelFragment() {
    @Inject lateinit var selectedSite: SelectedSite

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun getFragmentTitle() = getString(R.string.more_menu)

    override fun shouldExpandToolbar(): Boolean = false

    private val viewModel: MoreMenuViewModel by viewModels()

    override fun scrollToTop() {
        return
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            id = R.id.more_menu_compose_view
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    MoreMenuScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToSettingsEvent -> navigateToSettings()
                is StartSitePickerEvent -> startSitePicker()
                is ViewAdminEvent -> openInBrowser(event.url)
                is ViewStoreEvent -> openInBrowser(event.url)
                is ViewReviewsEvent -> navigateToReviews()
                is ViewInboxEvent -> navigateToInbox()
                is ViewCouponsEvent -> navigateToCoupons()
            }
        }
    }

    private fun navigateToSettings() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToSettingsActivity()
        )
    }

    private fun startSitePicker() {
        (requireActivity() as MainActivity).startSitePicker()
    }

    private fun openInBrowser(url: String) {
        ChromeCustomTabUtils.launchUrl(requireContext(), url)
    }

    private fun navigateToReviews() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToReviewList()
        )
    }

    private fun navigateToInbox() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuFragmentToInboxFragment()
        )
    }

    private fun navigateToCoupons() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToCouponListFragment()
        )
    }
}
