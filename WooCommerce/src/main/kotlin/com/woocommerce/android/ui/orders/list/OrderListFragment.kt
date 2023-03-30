package com.woocommerce.android.ui.orders.list

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.MenuProvider
import androidx.core.view.ViewGroupCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import com.woocommerce.android.AppConstants
import com.woocommerce.android.AppUrls
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentOrderListBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.pinFabAboveBottomNavigationBar
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.Feature.SIMPLE_PAYMENTS_AND_ORDER_CREATION
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tracker.OrderDurationRecorder
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.WooDialog.showDialog
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowOrderFilters
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject
import org.wordpress.android.util.ActivityUtils as WPActivityUtils

@AndroidEntryPoint
class OrderListFragment :
    TopLevelFragment(R.layout.fragment_order_list),
    OnQueryTextListener,
    OnActionExpandListener,
    OrderListListener,
    SwipeToComplete.OnSwipeListener,
    MenuProvider {
    companion object {
        const val TAG: String = "OrderListFragment"
        const val STATE_KEY_SEARCH_QUERY = "search-query"
        const val STATE_KEY_IS_SEARCHING = "is_searching"
        const val FILTER_CHANGE_NOTICE_KEY = "filters_changed_notice"
    }

    @Inject internal lateinit var uiMessageResolver: UIMessageResolver
    @Inject internal lateinit var selectedSite: SelectedSite
    @Inject internal lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: OrderListViewModel by viewModels()
    private var snackBar: Snackbar? = null

    override fun onStop() {
        snackBar?.dismiss()
        super.onStop()
    }

    // Alias for interacting with [viewModel.isSearching] so the value is always identical
    // to the real value on the UI side.
    private var isSearching: Boolean
        private set(value) {
            viewModel.isSearching = value
        }
        get() = viewModel.isSearching

    private var orderListMenu: Menu? = null
    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private val searchHandler = Handler(Looper.getMainLooper())

    private var _binding: FragmentOrderListBinding? = null
    private val binding get() = _binding!!

    // Alias for interacting with [viewModel.searchQuery] so the value is always identical
    // to the real value on the UI side.
    private var searchQuery: String
        private set(value) {
            viewModel.searchQuery = value
        }
        get() = viewModel.searchQuery

    private val emptyView
        get() = binding.orderListView.emptyView

    private val feedbackState
        get() = FeedbackPrefs.getFeatureFeedbackSettings(SIMPLE_PAYMENTS_AND_ORDER_CREATION)?.feedbackState
            ?: FeedbackState.UNANSWERED

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycle.addObserver(viewModel.performanceObserver)
        super.onCreate(savedInstanceState)

        savedInstanceState?.let { bundle ->
            isSearching = bundle.getBoolean(STATE_KEY_IS_SEARCHING)
            searchQuery = bundle.getString(STATE_KEY_SEARCH_QUERY, "")
        }

        val transitionDuration = resources.getInteger(R.integer.default_fragment_transition).toLong()
        val fadeThroughTransition = MaterialFadeThrough().apply { duration = transitionDuration }
        enterTransition = fadeThroughTransition
        exitTransition = fadeThroughTransition
        reenterTransition = fadeThroughTransition
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_order_list_fragment, menu)

        orderListMenu = menu
        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
        searchView?.queryHint = getSearchQueryHint()
    }

    private fun getSearchQueryHint(): String {
        return if (viewModel.viewState.isFilteringActive) {
            getString(R.string.orderlist_search_hint_active_filters)
        } else {
            getString(R.string.orderlist_search_hint)
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        refreshOptionsMenu()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderListBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        view.doOnPreDraw { startPostponedEnterTransition() }

        uiMessageResolver.anchorViewId = binding.createOrderButton.id

        binding.orderListView.init(currencyFormatter = currencyFormatter, orderListListener = this)
        ViewGroupCompat.setTransitionGroup(binding.orderRefreshLayout, true)
        binding.orderRefreshLayout.apply {
            // Set the scrolling view in the custom refresh SwipeRefreshLayout
            scrollUpChild = binding.orderListView.ordersList
            setOnRefreshListener {
                AnalyticsTracker.track(AnalyticsEvent.ORDERS_LIST_PULLED_TO_REFRESH)
                refreshOrders()
            }
        }

        initObservers()
        initializeResultHandlers()
        if (isSearching) {
            searchHandler.postDelayed({ searchView?.setQuery(searchQuery, true) }, 100)
        }
        binding.orderFiltersCard.setClickListener { viewModel.onFiltersButtonTapped() }
        initCreateOrderFAB(binding.createOrderButton)
        initSwipeBehaviour()
    }

    private fun initSwipeBehaviour() {
        val swipeToComplete = SwipeToComplete(requireContext(), this)
        val swipeHelper = ItemTouchHelper(swipeToComplete)
        swipeHelper.attachToRecyclerView(binding.orderListView.ordersList)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_KEY_IS_SEARCHING, isSearching)
        outState.putString(STATE_KEY_SEARCH_QUERY, searchQuery)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        disableSearchListeners()
        searchView = null
        orderListMenu = null
        searchMenuItem = null
        super.onDestroyView()
        _binding = null
    }

    /**
     * This is a replacement for activity?.invalidateOptionsMenu() since that causes the
     * search menu item to collapse
     */
    private fun refreshOptionsMenu() {
        if (!isChildFragmentShowing() && isSearching) {
            enableSearchListeners()
            val savedSearchQuery = searchQuery
            searchMenuItem?.expandActionView()
            searchQuery = savedSearchQuery
        } else {
            val showSearch = shouldShowSearchMenuItem()
            searchMenuItem?.let {
                if (it.isActionViewExpanded) it.collapseActionView()
                if (it.isVisible != showSearch) it.isVisible = showSearch
            }
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                AnalyticsTracker.track(AnalyticsEvent.ORDERS_LIST_MENU_SEARCH_TAPPED)
                enableSearchListeners()
                true
            }
            else -> false
        }
    }

    private fun initCreateOrderFAB(fabButton: FloatingActionButton) {
        fabButton.setOnClickListener { openOrderCreationFragment() }
        pinFabAboveBottomNavigationBar(fabButton)
    }

    private fun isChildFragmentShowing() = (activity as? MainNavigationRouter)?.isChildFragmentShowing() ?: false

    private fun shouldShowSearchMenuItem(): Boolean {
        val isChildShowing = isChildFragmentShowing()
        return when {
            (isChildShowing) -> false
            else -> true
        }
    }

    override fun getFragmentTitle() = if (isSearching) "" else getString(R.string.orders)

    override fun scrollToTop() {
        binding.orderListView.scrollToTop()
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun initObservers() {
        // setup observers
        viewModel.orderStatusOptions.observe(viewLifecycleOwner) {
            it?.let { options ->
                // So the order status can be matched to the appropriate label
                binding.orderListView.setOrderStatusOptions(options)
            }
        }

        viewModel.isFetchingFirstPage.observe(viewLifecycleOwner) {
            binding.orderRefreshLayout.isRefreshing = it == true
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) {
            it?.let { isLoadingMore ->
                binding.orderListView.setLoadingMoreIndicator(active = isLoadingMore)
            }
        }

        viewModel.pagedListData.observe(viewLifecycleOwner) {
            updatePagedListData(it)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowErrorSnack -> {
                    uiMessageResolver.showSnack(event.messageRes)
                    binding.orderRefreshLayout.isRefreshing = false
                }
                is ShowOrderFilters -> showOrderFilters()
                is OrderListViewModel.OrderListEvent.OpenPurchaseCardReaderLink -> {
                    findNavController().navigate(
                        NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                            urlToLoad = event.url,
                            title = resources.getString(event.titleRes)
                        )
                    )
                }
                is OrderListViewModel.OrderListEvent.OpenIPPFeedbackSurveyLink -> {
                    NavGraphMainDirections
                        .actionGlobalFeedbackSurveyFragment(customUrl = event.url)
                        .apply { findNavController().navigateSafely(this) }
                }
                is OrderListViewModel.OrderListEvent.NotifyOrderChanged -> {
                    binding.orderListView.ordersList.adapter?.notifyItemChanged(event.position)
                }
                is MultiLiveEvent.Event.ShowUndoSnackbar -> {
                    snackBar = uiMessageResolver.getUndoSnack(
                        message = event.message,
                        actionListener = event.undoAction
                    ).also {
                        it.addCallback(event.dismissAction)
                        it.show()
                    }
                }
                is OrderListViewModel.OrderListEvent.ShowRetryErrorSnack -> {
                    snackBar = uiMessageResolver.getRetrySnack(
                        message = event.message,
                        actionListener = event.retry
                    ).also {
                        it.show()
                    }
                    binding.orderRefreshLayout.isRefreshing = false
                }
                is OrderListViewModel.OrderListEvent.ShowIPPDismissConfirmationDialog -> {
                    showIPPFeedbackDismissConfirmationDialog()
                }
                else -> event.isHandled = false
            }
        }

        viewModel.emptyViewType.observe(viewLifecycleOwner) {
            it?.let { emptyViewType ->
                when (emptyViewType) {
                    EmptyViewType.SEARCH_RESULTS -> {
                        emptyView.show(emptyViewType, searchQueryOrFilter = searchQuery)
                    }
                    EmptyViewType.ORDER_LIST -> {
                        emptyView.show(emptyViewType) {
                            ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.URL_LEARN_MORE_ORDERS)
                        }
                    }
                    EmptyViewType.ORDER_LIST_FILTERED -> {
                        emptyView.show(emptyViewType)
                    }
                    EmptyViewType.NETWORK_OFFLINE, EmptyViewType.NETWORK_ERROR -> {
                        emptyView.show(emptyViewType) {
                            refreshOrders()
                        }
                    }
                    else -> {
                        emptyView.show(emptyViewType)
                    }
                }
            } ?: hideEmptyView()
        }

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.filterCount.takeIfNotEqualTo(old?.filterCount) { filterCount ->
                binding.orderFiltersCard.updateFilterSelection(filterCount)
            }
            new.ippFeedbackBannerState.takeIfNotEqualTo(old?.ippFeedbackBannerState) { bannerState ->
                renderIPPBanner(bannerState)
            }
            new.isSimplePaymentsWIPNoticeCardVisible.takeIfNotEqualTo(old?.isSimplePaymentsWIPNoticeCardVisible) {
                displaySimplePaymentsWIPCard(it)
            }
        }
    }

    private fun showIPPFeedbackDismissConfirmationDialog() {
        showDialog(
            activity = requireActivity(),
            titleId = R.string.feedback_banner_ipp_dismiss_confirmation_title,
            messageId = R.string.feedback_banner_ipp_dismiss_confirmation_message,
            positiveButtonId = R.string.feedback_banner_ipp_dismiss_confirmation_remind_later_button,
            negativeButtonId = R.string.feedback_banner_ipp_dismiss_confirmation_remind_later_dont_show_again_button,
            negBtnAction = { _, _ -> viewModel.onIPPFeedbackBannerDismissedForever() },
            posBtnAction = { _, _ -> viewModel.onIPPFeedbackBannerDismissedShowLater() }
        )
    }

    private fun renderIPPBanner(bannerState: OrderListViewModel.IPPSurveyFeedbackBannerState) {
        val isVisible =
            bannerState is OrderListViewModel.IPPSurveyFeedbackBannerState.Visible && !DisplayUtils.isLandscape(context)

        binding.ippFeedbackBanner.isVisible = isVisible

        if (isVisible) {
            val data =
                (bannerState as OrderListViewModel.IPPSurveyFeedbackBannerState.Visible).bannerData
            binding.ippFeedbackBanner.setMessage(data.message)
            binding.ippFeedbackBanner.setTitle(data.title)
            binding.ippFeedbackBanner.onDismissClickListener = {
                viewModel.onDismissIPPFeedbackBannerClicked()
            }
            binding.ippFeedbackBanner.onCTAClickListener = {
                viewModel.onIPPFeedbackBannerCTAClicked()
            }
        }
    }

    private fun initializeResultHandlers() {
        handleResult<String>(FILTER_CHANGE_NOTICE_KEY) {
            viewModel.loadOrders()
        }
    }

    private fun showOrderFilters() {
        findNavController().navigateSafely(R.id.action_orderListFragment_to_orderFilterListFragment)
    }

    private fun openOrderCreationFragment() {
        OrderDurationRecorder.startRecording()
        AnalyticsTracker.track(AnalyticsEvent.ORDERS_ADD_NEW)
        findNavController().navigateSafely(
            OrderListFragmentDirections.actionOrderListFragmentToOrderCreationFragment(
                OrderCreateEditViewModel.Mode.Creation
            )
        )
    }

    private fun hideEmptyView() {
        emptyView.hide()
    }

    private fun updatePagedListData(pagedListData: PagedList<OrderListItemUIType>?) {
        binding.orderListView.submitPagedList(pagedListData)
    }

    override fun openOrderDetail(orderId: Long, orderStatus: String, sharedView: View?) {
        viewModel.trackOrderClickEvent(orderId, orderStatus)

        // if a search is active, we need to collapse the search view so order detail can show it's title and then
        // remember the user was searching (since both searchQuery and isSearching will be reset)
        if (isSearching) {
            val savedSearch = searchQuery
            clearSearchResults()
            updateActivityTitle()
            searchQuery = savedSearch
            isSearching = true
        }
        (activity as? MainNavigationRouter)?.run {
            if (sharedView != null) {
                showOrderDetailWithSharedTransition(
                    orderId = orderId,
                    sharedView = sharedView
                )
            } else {
                showOrderDetail(orderId)
            }
        }
    }

    // region search
    override fun onQueryTextSubmit(query: String): Boolean {
        handleNewSearchRequest(query)
        WPActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        // only display the order status list if the search query is empty
        if (newText.isEmpty()) {
            searchQuery = ""
        }

        if (newText.length > 2) {
            submitSearchDelayed(newText)
        } else {
            binding.orderListView.clearAdapterData()
        }
        return true
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        isSearching = true
        checkOrientation()
        onSearchViewActiveChanged(isActive = true)
        binding.orderFiltersCard.isVisible = false
        binding.orderListView.clearAdapterData()
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        clearSearchResults()
        searchMenuItem?.isVisible = true
        viewModel.onSearchClosed()
        binding.orderFiltersCard.isVisible = true
        onSearchViewActiveChanged(isActive = false)
        return true
    }

    private fun clearSearchResults() {
        if (isSearching) {
            searchQuery = ""
            isSearching = false
            disableSearchListeners()
            updateActivityTitle()
            searchMenuItem?.collapseActionView()
            (activity as? MainActivity)?.showBottomNav()
        }
    }

    /**
     * Submit the search after a brief delay unless the query has changed - this is used to
     * perform a search while the user is typing
     */
    private fun submitSearchDelayed(query: String) {
        searchHandler.postDelayed(
            {
                searchView?.let {
                    // submit the search if the searchView's query still matches the passed query
                    if (query == it.query.toString()) handleNewSearchRequest(query)
                }
            },
            AppConstants.SEARCH_TYPING_DELAY_MS
        )
    }

    /**
     * Only fired while the user is actively typing in the search
     * field.
     */
    private fun handleNewSearchRequest(query: String) {
        AnalyticsTracker.track(
            AnalyticsEvent.ORDERS_LIST_SEARCH,
            mapOf(AnalyticsTracker.KEY_SEARCH to query)
        )

        searchQuery = query
        submitSearchQuery(searchQuery)
    }

    /**
     * Loads a new list with the search query. This can be called while the
     * user is interacting with the search component, or to reload the
     * view state.
     */
    private fun submitSearchQuery(query: String) {
        viewModel.submitSearchOrFilter(searchQuery = query)
    }

    private fun refreshOrders() {
        viewModel.fetchOrdersAndOrderDependencies()
    }

    private fun disableSearchListeners() {
        binding.orderListViewRoot.visibility = View.VISIBLE
        searchMenuItem?.setOnActionExpandListener(null)
        searchView?.setOnQueryTextListener(null)
    }

    /**
     * Method called when user clicks on the search menu icon.
     *
     * 1. The settings menu is hidden when the search filter is active to prevent the search view
     *    getting collapsed if the settings menu from the [MainActivity] is clicked.
     * 2. The order status list view is displayed by default
     */
    private fun enableSearchListeners() {
        hideEmptyView()

        searchMenuItem?.setOnActionExpandListener(this)
        searchView?.setOnQueryTextListener(this)

        (activity as? MainActivity)?.hideBottomNav()
    }

    private fun checkOrientation() {
        val isLandscape = DisplayUtils.isLandscape(context)
        if (isLandscape && isSearching) {
            searchView?.post { searchView?.clearFocus() }
        }
    }
    // endregion

    override fun shouldExpandToolbar(): Boolean {
        return binding.orderListView.ordersList.computeVerticalScrollOffset() == 0 && !isSearching
    }

    private fun displaySimplePaymentsWIPCard(show: Boolean) {
        if (!show || feedbackState == FeedbackState.DISMISSED) {
            binding.simplePaymentsWIPcard.isVisible = false
            return
        }

        binding.simplePaymentsWIPcard.isVisible = true
        binding.simplePaymentsWIPcard.initView(
            getString(R.string.orderlist_simple_payments_wip_title),
            getString(R.string.orderlist_simple_payments_wip_message_enabled),
            onGiveFeedbackClick = { onGiveFeedbackClicked() },
            onDismissClick = { onDismissWIPCardClicked() },
            showFeedbackButton = true
        )
    }

    private fun onGiveFeedbackClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
            )
        )
        registerFeedbackSetting(FeedbackState.GIVEN)
        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.ORDER_CREATION)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissWIPCardClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
            )
        )
        registerFeedbackSetting(FeedbackState.DISMISSED)
        displaySimplePaymentsWIPCard(false)
    }

    private fun registerFeedbackSetting(state: FeedbackState) {
        FeatureFeedbackSettings(
            SIMPLE_PAYMENTS_AND_ORDER_CREATION,
            state
        ).registerItself()
    }

    override fun onSwiped(gestureSource: OrderStatusUpdateSource.SwipeToCompleteGesture) {
        viewModel.onSwipeStatusUpdate(gestureSource)
    }
}
