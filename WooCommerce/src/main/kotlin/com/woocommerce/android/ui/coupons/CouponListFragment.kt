package com.woocommerce.android.ui.coupons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView.OnQueryTextListener
import androidx.appcompat.widget.SearchView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentCouponListBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.Feature.COUPONS
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.coupons.CouponListViewModel.NavigateToCouponDetailsEvent
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CouponListFragment : BaseFragment(R.layout.fragment_coupon_list) {
    companion object {
        const val TAG: String = "CouponListFragment"
    }

    private lateinit var searchMenuItem: MenuItem
    private lateinit var searchView: SearchView
    private var _binding: FragmentCouponListBinding? = null
    private val binding get() = _binding!!
    private val feedbackState
        get() = FeedbackPrefs.getFeatureFeedbackSettings(COUPONS)?.feedbackState
            ?: FeatureFeedbackSettings.FeedbackState.UNANSWERED

    private val viewModel: CouponListViewModel by viewModels()
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentCouponListBinding.inflate(inflater, container, false)

        val view = binding.root
        binding.couponsComposeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    CouponListScreen(viewModel)
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
        viewModel.couponsState.observe(viewLifecycleOwner) { state ->
            if (::searchMenuItem.isInitialized && state.isSearchOpen != searchMenuItem.isActionViewExpanded) {
                if (state.isSearchOpen) searchMenuItem.expandActionView() else searchMenuItem.collapseActionView()
            }
            if (::searchView.isInitialized && state.isSearchOpen && state.searchQuery != searchView.query?.toString()) {
                searchView.setQuery(state.searchQuery, false)
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToCouponDetailsEvent -> navigateToCouponDetails(event.couponId)
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_search, menu)
        searchMenuItem = menu.findItem(R.id.menu_search)
        initSearch()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        displayCouponsWIPCard(true)
    }

    override fun getFragmentTitle(): String = getString(R.string.coupons)

    private fun initSearch() {
        searchView = searchMenuItem.actionView as SearchView
        searchView.queryHint = getString(R.string.coupons_list_search_hint)
        viewModel.couponsState.value?.let {
            if (it.isSearchOpen) {
                searchMenuItem.expandActionView()
                searchView.setQuery(it.searchQuery, false)
            } else {
                searchMenuItem.collapseActionView()
            }
        }
        val textQueryListener = object : OnQueryTextListener, SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (isAdded) {
                    viewModel.onSearchQueryChanged(query.orEmpty())
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (isAdded) {
                    viewModel.onSearchQueryChanged(newText.orEmpty())
                }
                return true
            }
        }
        searchMenuItem.setOnActionExpandListener(object : OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                if (isAdded) {
                    viewModel.onSearchStateChanged(open = true)
                    searchView.setOnQueryTextListener(textQueryListener)
                }
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                if (isAdded) {
                    searchView.setOnQueryTextListener(null)
                    viewModel.onSearchStateChanged(open = false)
                }
                return true
            }
        })
    }

    private fun navigateToCouponDetails(couponId: Long) {
        findNavController().navigateSafely(
            CouponListFragmentDirections.actionCouponListFragmentToCouponDetailsFragment(couponId)
        )
    }

    private fun displayCouponsWIPCard(show: Boolean) {
        if (!show ||
            feedbackState == FeatureFeedbackSettings.FeedbackState.DISMISSED
        ) {
            binding.couponsWIPcard.isVisible = false
            return
        }

        binding.couponsWIPcard.isVisible = true
        binding.couponsWIPcard.initView(
            getString(R.string.coupon_list_wip_title),
            getString(R.string.coupon_list_wip_message_enabled),
            onGiveFeedbackClick = { onGiveFeedbackClicked() },
            onDismissClick = { onDismissWIPCardClicked() },
            showFeedbackButton = true
        )
    }

    private fun onGiveFeedbackClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_COUPONS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
            )
        )
        registerFeedbackSetting(FeatureFeedbackSettings.FeedbackState.GIVEN)

        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.COUPONS)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissWIPCardClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_COUPONS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
            )
        )
        registerFeedbackSetting(FeatureFeedbackSettings.FeedbackState.DISMISSED)
        displayCouponsWIPCard(false)
    }

    private fun registerFeedbackSetting(state: FeatureFeedbackSettings.FeedbackState) {
        FeatureFeedbackSettings(
            COUPONS,
            state
        ).registerItself()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
