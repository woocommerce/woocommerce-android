package com.woocommerce.android.ui.orders.details

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.FEATURE_FEEDBACK_BANNER
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_DETAIL_PRODUCT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ORDER_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_START_PAYMENT_FLOW
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.databinding.FragmentOrderDetailBinding
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.handleDialogNotice
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.extensions.whenNotNullNorEmpty
import com.woocommerce.android.extensions.windowSizeClass
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.Feature.SHIPPING_LABEL_M4
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.DISMISSED
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.GIVEN
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.UNANSWERED
import com.woocommerce.android.model.GiftCardSummary
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.Subscription
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.CustomAmountCard
import com.woocommerce.android.ui.orders.Header
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.OrderNavigator
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.OrdersCommunicationViewModel
import com.woocommerce.android.ui.orders.OrdersCommunicationViewModel.CommunicationEvent.OrdersEmptyNotified
import com.woocommerce.android.ui.orders.OrdersCommunicationViewModel.CommunicationEvent.OrdersLoadingNotified
import com.woocommerce.android.ui.orders.creation.shipping.ShippingLineDetails
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShippingLabelsAdapter.OnShippingLabelClickListener
import com.woocommerce.android.ui.orders.details.editing.OrderEditingViewModel
import com.woocommerce.android.ui.orders.details.views.OrderDetailAttributionInfoView
import com.woocommerce.android.ui.orders.details.views.OrderDetailOrderStatusView.Mode
import com.woocommerce.android.ui.orders.fulfill.OrderFulfillViewModel
import com.woocommerce.android.ui.orders.list.OrderListFragment
import com.woocommerce.android.ui.orders.notes.AddOrderNoteFragment
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelFragment
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRefundFragment
import com.woocommerce.android.ui.orders.tracking.AddOrderShipmentTrackingFragment
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentDialogFragment
import com.woocommerce.android.ui.payments.refunds.RefundSummaryFragment
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.model.OrderAttributionInfo
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@Suppress("LargeClass")
@AndroidEntryPoint
class OrderDetailFragment :
    BaseFragment(R.layout.fragment_order_detail),
    OrderProductActionListener {
    companion object {
        val TAG: String = OrderDetailFragment::class.java.simpleName
        private const val MARGINS_FOR_TABLET: Float = 0.1F
        private const val MARGINS_FOR_SMALL_TABLET_PORTRAIT: Float = 0.025F
    }

    private val viewModel: OrderDetailViewModel by viewModels()
    private val orderEditingViewModel by fixedHiltNavGraphViewModels<OrderEditingViewModel>(R.id.nav_graph_orders)
    private val communicationViewModel: OrdersCommunicationViewModel by activityViewModels()

    @Inject
    lateinit var navigator: OrderNavigator

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var productImageMap: ProductImageMap

    @Inject
    lateinit var dateUtils: DateUtils

    @Inject
    lateinit var cardReaderManager: CardReaderManager

    @Inject
    lateinit var feedbackPrefs: FeedbackPrefs

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private val skeletonView = SkeletonView()
    private var undoSnackbar: Snackbar? = null

    private val navArgs: OrderDetailFragmentArgs by navArgs()

    private var screenTitle = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

    private val feedbackState
        get() = feedbackPrefs.getFeatureFeedbackSettings(SHIPPING_LABEL_M4)?.feedbackState
            ?: UNANSWERED

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        undoSnackbar?.dismiss()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycle.addObserver(viewModel.performanceObserver)
        super.onCreate(savedInstanceState)
        val transitionDuration = resources.getInteger(R.integer.default_fragment_transition).toLong()
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.default_window_background)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.snack_root
            duration = transitionDuration
            scrimColor = Color.TRANSPARENT
            startContainerColor = backgroundColor
            endContainerColor = backgroundColor
        }
    }

    override fun getFragmentTitle() = screenTitle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * In tablet split view, when the app window is initially narrow,
         * the order detail occupies the full screen as a single pane.
         * Below code takes care of transition handling:
         * if the app window is then expanded in the split view,
         * the layout should adapt from the single-pane full-screen mode to a two-pane layout,
         * ensuring a seamless user experience across varying app window sizes.
         *
         * This code identifies scenarios where the device is a tablet and the order detail currently
         * occupies the entire window (typical in a transition from single-pane to two-pane layout).
         * It then navigates up to the order list screen, which is responsible for managing the two-pane
         * layout effectively.
         *
         * The code also determines if the Order Detail screen is invoked following order creation
         * during the payment collection process. If this is the case, it navigates to the
         * Select Payment screen on both phone and tablet devices.
         */
        val isScreenLargerThanCompact = requireContext().windowSizeClass != WindowSizeClass.Compact
        if (isOrderListFragmentNotVisible() && isScreenLargerThanCompact && !navArgs.startPaymentFlow) {
            navigateBackWithResult(KEY_ORDER_ID, navArgs.orderId)
            return
        } else if (isOrderListFragmentNotVisible() && isScreenLargerThanCompact && navArgs.startPaymentFlow) {
            navigateBackWithResult(KEY_START_PAYMENT_FLOW, navArgs.orderId)
            return
        }

        _binding = FragmentOrderDetailBinding.bind(view)

        setMarginsIfTablet()
        setupToolbar()

        setupObservers(viewModel)
        setupOrderEditingObservers(orderEditingViewModel)
        setupResultHandlers(viewModel)
        setupOrdersCommunicationObservers(communicationViewModel)

        binding.orderDetailOrderStatus.initView(mode = Mode.OrderEdit) {
            viewModel.onEditOrderStatusSelected()
        }
        binding.orderRefreshLayout.apply {
            scrollUpChild = binding.scrollView
            setOnRefreshListener { viewModel.onRefreshRequested() }
        }
        binding.customFieldsCard.customFieldsButton.setOnClickListener {
            viewModel.onCustomFieldsButtonClicked()
        }
        binding.orderDetailsAICard.aiThankYouNoteButton.setOnClickListener {
            viewModel.onAIThankYouNoteButtonClicked()
        }
        binding.orderDetailTrash.setOnClickListener {
            viewModel.onTrashOrderClicked()
        }

        ViewCompat.setTransitionName(
            binding.scrollView,
            getString(R.string.order_card_detail_transition_name)
        )
    }

    private fun isOrderListFragmentNotVisible() = parentFragment?.parentFragment !is OrderListFragment

    private fun setMarginsIfTablet() {
        val windowWidth = DisplayUtils.getWindowPixelWidth(requireContext())
        val layoutParams = binding.orderDetailContainer.layoutParams as FrameLayout.LayoutParams
        when (requireContext().windowSizeClass) {
            WindowSizeClass.Medium -> {
                val marginHorizontal = (windowWidth * MARGINS_FOR_SMALL_TABLET_PORTRAIT).toInt()
                layoutParams.setMargins(
                    marginHorizontal,
                    layoutParams.topMargin,
                    marginHorizontal,
                    layoutParams.bottomMargin
                )
            }

            WindowSizeClass.Expanded, WindowSizeClass.Large -> {
                val marginHorizontal = (windowWidth * MARGINS_FOR_TABLET).toInt()
                layoutParams.setMargins(
                    marginHorizontal,
                    layoutParams.topMargin,
                    marginHorizontal,
                    layoutParams.bottomMargin
                )
            }

            WindowSizeClass.Compact -> return
        }
        binding.orderDetailContainer.layoutParams = layoutParams
    }

    private fun setupToolbar() {
        binding.toolbar.title = screenTitle
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            onMenuItemSelected(menuItem)
        }
        // Set up the toolbar menu
        binding.toolbar.inflateMenu(R.menu.menu_order_detail)
        setupToolbarMenu(binding.toolbar.menu)
    }

    private fun setupToolbarMenu(menu: Menu) {
        onPrepareMenu(menu)
        if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
            binding.toolbar.navigationIcon = null
        } else {
            binding.toolbar.navigationIcon = AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_back_24dp)
            binding.toolbar.setNavigationOnClickListener {
                if (!findNavController().popBackStack(R.id.orders, false)) {
                    // in case the back stack is empty, indicating that the OrderDetailsFragment is shown in details pane
                    // of the OrderListFragment, we need to propagate back press to the parent fragment manually.
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        val menuEditOrder = menu.findItem(R.id.menu_edit_order)
        menuEditOrder.isVisible = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.menu_edit_order)?.let {
            it.isEnabled = viewModel.hasOrder()
        }

        menu.findItem(R.id.menu_arrow_up)?.let {
            it.isVisible = viewModel.orderNavigationIsEnabled()

            if (it.isVisible) {
                it.isEnabled = viewModel.previousOrderNavigationIsEnabled()
            }
        }

        menu.findItem(R.id.menu_arrow_down)?.let {
            it.isVisible = viewModel.orderNavigationIsEnabled()

            if (it.isVisible) {
                it.isEnabled = viewModel.nextOrderNavigationIsEnabled()
            }
        }
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit_order -> {
                viewModel.onEditClicked()
                true
            }

            R.id.menu_arrow_up -> {
                viewModel.onPreviousOrderClicked()
                true
            }

            R.id.menu_arrow_down -> {
                viewModel.onNextOrderClicked()
                true
            }
            else -> {
                false
            }
        }
    }

    override fun openOrderProductDetail(remoteProductId: Long) {
        AnalyticsTracker.track(ORDER_DETAIL_PRODUCT_TAPPED)
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }

    override fun openOrderProductVariationDetail(remoteProductId: Long, remoteVariationId: Long) {
        AnalyticsTracker.track(ORDER_DETAIL_PRODUCT_TAPPED)
        (activity as? MainNavigationRouter)?.showProductVariationDetail(remoteProductId, remoteVariationId)
    }

    private fun setupOrdersCommunicationObservers(ordersCommunicationViewModel: OrdersCommunicationViewModel) {
        ordersCommunicationViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OrdersEmptyNotified -> {
                    viewModel.showEmptyView()
                }

                is OrdersLoadingNotified -> {
                    viewModel.showLoadingView()
                }

                else -> event.isHandled = false
            }
        }
    }

    private fun setupObservers(viewModel: OrderDetailViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.orderInfo?.takeIfNotEqualTo(old?.orderInfo) {
                showOrderDetail(it.order!!, it.isPaymentCollectableWithCardReader, it.receiptButtonStatus)
                if (requireContext().windowSizeClass != WindowSizeClass.Compact) {
                    orderEditingViewModel.setOrderId(it.order.id)
                }
                onPrepareMenu(binding.toolbar.menu)
            }
            new.orderStatus?.takeIfNotEqualTo(old?.orderStatus) { showOrderStatus(it) }
            new.isMarkOrderCompleteButtonVisible?.takeIfNotEqualTo(old?.isMarkOrderCompleteButtonVisible) {
                showMarkOrderCompleteButton(it)
            }
            new.isCreateShippingLabelButtonVisible?.takeIfNotEqualTo(old?.isCreateShippingLabelButtonVisible) {
                showShippingLabelButton(it)
            }
            new.isProductListMenuVisible?.takeIfNotEqualTo(old?.isProductListMenuVisible) {
                showProductListMenuButton(it)
            }
            new.isCreateShippingLabelBannerVisible.takeIfNotEqualTo(old?.isCreateShippingLabelBannerVisible) {
                displayShippingLabelsWIPCard(it)
            }
            new.isProductListVisible?.takeIfNotEqualTo(old?.isProductListVisible) {
                binding.orderDetailProductList.isVisible = it
            }
            new.toolbarTitle?.takeIfNotEqualTo(old?.toolbarTitle) {
                screenTitle = it
                binding.toolbar.title = it
            }
            new.isOrderDetailSkeletonShown?.takeIfNotEqualTo(old?.isOrderDetailSkeletonShown) { showSkeleton(it) }
            new.isShipmentTrackingAvailable?.takeIfNotEqualTo(old?.isShipmentTrackingAvailable) {
                showAddShipmentTracking(it)
            }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                binding.orderRefreshLayout.isRefreshing = it
            }
            new.refreshedProductId?.takeIfNotEqualTo(old?.refreshedProductId) { refreshProduct(it) }
            new.wcShippingBannerVisible?.takeIfNotEqualTo(old?.wcShippingBannerVisible) {
                showInstallWcShippingBanner(it)
            }
            new.isCustomFieldsButtonShown?.takeIfNotEqualTo(old?.isCustomFieldsButtonShown) {
                binding.customFieldsCard.isVisible = it
            }
            new.isAIThankYouNoteButtonShown.takeIfNotEqualTo(old?.isAIThankYouNoteButtonShown) {
                binding.orderDetailsAICard.isVisible = it
            }
            new.isOrderDetailEmpty.takeIfNotEqualTo(old?.isOrderDetailEmpty) { showEmptyView(it) }
        }

        viewModel.orderNotes.observe(viewLifecycleOwner) {
            showOrderNotes(it)
        }
        viewModel.orderRefunds.observe(viewLifecycleOwner) {
            showOrderRefunds(it, viewModel.order)
        }
        viewModel.productList.observe(viewLifecycleOwner) {
            showOrderProducts(it, viewModel.order.currency)
        }
        showCustomAmounts(viewModel.feeLineList)
        viewModel.shipmentTrackings.observe(viewLifecycleOwner) {
            showShipmentTrackings(it)
        }
        viewModel.shippingLabels.observe(viewLifecycleOwner) {
            showShippingLabels(it, viewModel.order.currency)
        }
        viewModel.subscriptions.observe(viewLifecycleOwner) {
            showSubscriptions(it)
        }
        viewModel.giftCards.observe(viewLifecycleOwner) {
            showGiftCards(it, viewModel.order.currency)
        }
        showShippingLines(viewModel.shippingLineList)

        setupOrderAttributionInfoCard(viewModel.orderAttributionInfo)

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> {
                    if (event.args.isNotEmpty()) {
                        uiMessageResolver.getSnack(event.message, *event.args).show()
                    } else {
                        uiMessageResolver.showSnack(event.message)
                    }
                }
                is ShowUndoSnackbar -> {
                    displayUndoSnackbar(event.message, event.undoAction, event.dismissAction)
                }
                is OrderNavigationTarget -> navigator.navigate(this, event)
                is InstallWCShippingViewModel.InstallWcShipping -> navigateToInstallWcShippingFlow()
                is OrderDetailViewModel.TrashOrder -> {
                    if (findNavController().previousBackStackEntry != null) {
                        findNavController().popBackStack()
                    }

                    communicationViewModel.trashOrder(event.orderId)
                }
                is MultiLiveEvent.Event.ShowDialog -> event.showDialog()
                else -> event.isHandled = false
            }
        }
        viewModel.start()
    }

    private fun showShippingLines(shippingLineList: LiveData<List<ShippingLineDetails>>) {
        binding.orderDetailShippingLines.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                shippingLineList.observeAsState().value?.let { shippingLines ->
                    WooThemeWithBackground {
                        ShippingLineSection(
                            shippingLineDetails = shippingLines,
                            formatCurrency = { amount -> currencyFormatter.formatCurrency(amount) },
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
            }
        }
    }

    private fun showSubscriptions(subscriptions: List<Subscription>) {
        binding.orderDetailSubscriptionList.run {
            updateSubscriptionList(
                subscriptions = subscriptions,
                currencyFormatter = currencyFormatter
            )

            // Animate visibility only when necessary
            if (subscriptions.isEmpty() && visibility == View.GONE) return

            TransitionManager.endTransitions(binding.orderDetailContainer)
            TransitionManager.beginDelayedTransition(binding.orderDetailContainer)
            visibility = if (subscriptions.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showGiftCards(giftCardSummaries: List<GiftCardSummary>, currencyCode: String) {
        binding.orderDetailGiftCardList.run {
            updateGiftCardList(
                giftCards = giftCardSummaries,
                currencyFormatter = currencyFormatter,
                currencyCode = currencyCode
            )

            // Animate visibility only when necessary
            if (giftCardSummaries.isEmpty() && visibility == View.GONE) return@run

            TransitionManager.endTransitions(binding.orderDetailContainer)
            TransitionManager.beginDelayedTransition(binding.orderDetailContainer)

            visibility = if (giftCardSummaries.isNotEmpty()) View.VISIBLE else View.GONE
        }
        binding.orderDetailPaymentInfo.updateGiftCardSection(
            giftCardSummaries = giftCardSummaries,
            formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(currencyCode)
        )
    }

    private fun setupOrderAttributionInfoCard(orderAttributionInfo: LiveData<OrderAttributionInfo>) {
        binding.orderDetailOrderAttributionInfo.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                orderAttributionInfo.observeAsState().value?.let {
                    WooThemeWithBackground {
                        OrderDetailAttributionInfoView(attributionInfo = it)
                    }
                }
            }
        }
    }

    private fun navigateToInstallWcShippingFlow() {
        findNavController().navigateSafely(
            OrderDetailFragmentDirections.actionOrderDetailFragmentToInstallWcShippingFlow()
        )
    }

    private fun showInstallWcShippingBanner(isVisible: Boolean) {
        val banner = binding.orderDetailInstallWcShippingBanner
        banner.isVisible = isVisible && FeatureFlag.WC_SHIPPING_BANNER.isEnabled()
        banner.setClickListeners(
            onInstallWcShipping = { viewModel.onGetWcShippingClicked() },
            onDismiss = { viewModel.onWcShippingBannerDismissed() }
        )
    }

    private fun setupOrderEditingObservers(orderEditingViewModel: OrderEditingViewModel) {
        orderEditingViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OrderEditingViewModel.OrderEdited -> viewModel.onOrderEdited()
                is OrderEditingViewModel.OrderEditFailed -> viewModel.onOrderEditFailed(event.message)
            }
        }
    }

    private fun setupResultHandlers(viewModel: OrderDetailViewModel) {
        handleDialogResult<OrderStatusUpdateSource>(
            key = OrderStatusSelectorDialog.KEY_ORDER_STATUS_RESULT,
            entryId = R.id.orderDetailFragment
        ) { updateSource ->
            viewModel.onOrderStatusChanged(updateSource)
        }
        handleResult<OrderNote>(AddOrderNoteFragment.KEY_ADD_NOTE_RESULT) {
            viewModel.onNewOrderNoteAdded(it)
        }
        handleResult<Boolean>(ShippingLabelRefundFragment.KEY_REFUND_SHIPPING_LABEL_RESULT) {
            viewModel.onShippingLabelRefunded()
        }
        handleResult<OrderShipmentTracking>(AddOrderShipmentTrackingFragment.KEY_ADD_SHIPMENT_TRACKING_RESULT) {
            viewModel.onNewShipmentTrackingAdded(it)
        }
        handleResult<OrderStatusUpdateSource>(OrderFulfillViewModel.KEY_ORDER_FULFILL_RESULT) { updateSource ->
            viewModel.onOrderStatusChanged(updateSource)
        }
        handleResult<Boolean>(OrderFulfillViewModel.KEY_REFRESH_SHIPMENT_TRACKING_RESULT) {
            viewModel.refreshShipmentTracking()
        }
        handleDialogNotice(
            key = CardReaderPaymentDialogFragment.KEY_CARD_PAYMENT_RESULT,
            entryId = R.id.orderDetailFragment
        ) {
            viewModel.onCardReaderPaymentCompleted()
        }
        handleNotice(RefundSummaryFragment.REFUND_ORDER_NOTICE_KEY) {
            viewModel.onOrderItemRefunded()
        }
        handleNotice(PrintShippingLabelFragment.KEY_LABEL_PURCHASED) {
            viewModel.onShippingLabelsPurchased()
        }
    }

    private fun showOrderDetail(
        order: Order,
        isPaymentCollectableWithCardReader: Boolean,
        receiptButtonStatus: OrderDetailViewState.ReceiptButtonStatus
    ) {
        binding.orderDetailOrderStatus.updateOrder(order)
        binding.orderDetailCustomerInfo.updateCustomerInfo(
            order = order,
            isVirtualOrder = viewModel.hasVirtualProductsOnly(),
            isReadOnly = false
        )
        binding.orderDetailPaymentInfo.updatePaymentInfo(
            order = order,
            isPaymentCollectableWithCardReader = isPaymentCollectableWithCardReader,
            receiptButtonStatus = receiptButtonStatus,
            formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency),
            onIssueRefundClickListener = { viewModel.onIssueOrderRefundClicked() },
            onSeeReceiptClickListener = {
                viewModel.onSeeReceiptClicked()
            },
            onCollectPaymentClickListener = {
                viewModel.onCollectPaymentClicked(requireContext().windowSizeClass != WindowSizeClass.Compact)
            },
            onPrintingInstructionsClickListener = {
                viewModel.onPrintingInstructionsClicked()
            }
        )
    }

    private fun showOrderStatus(orderStatus: OrderStatus) {
        binding.orderDetailOrderStatus.updateStatus(orderStatus)
    }

    private fun showMarkOrderCompleteButton(isVisible: Boolean) {
        binding.orderDetailProductList.showMarkOrderCompleteButton(
            isVisible,
            viewModel::onMarkOrderCompleteButtonTapped
        )
    }

    private fun showShippingLabelButton(isVisible: Boolean) {
        binding.orderDetailProductList.showCreateShippingLabelButton(
            isVisible,
            viewModel::onCreateShippingLabelButtonTapped,
            viewModel::onShippingLabelNoticeTapped
        )
    }

    private fun showProductListMenuButton(isVisible: Boolean) {
        binding.orderDetailProductList.showProductListMenuButton(isVisible)
    }

    private fun showSkeleton(show: Boolean) {
        when (show) {
            true -> skeletonView.show(binding.orderDetailContainer, R.layout.skeleton_order_detail, delayed = true)
            false -> skeletonView.hide()
        }
    }

    private fun refreshProduct(remoteProductId: Long) {
        binding.orderDetailProductList.notifyProductChanged(remoteProductId)
    }

    private fun showOrderNotes(orderNotes: List<OrderNote>) {
        binding.orderDetailNoteList.updateOrderNotesView(orderNotes) {
            viewModel.onAddOrderNoteClicked()
        }
    }

    private fun showOrderRefunds(refunds: List<Refund>, order: Order) {
        // display the refunds count in the refunds section
        val refundsCount = refunds.sumOf { refund -> refund.items.sumOf { it.quantity } }
        if (refundsCount > 0) {
            binding.orderDetailRefundsInfo.show()
            binding.orderDetailRefundsInfo.updateRefundCount(refundsCount) {
                viewModel.onViewRefundedProductsClicked()
            }
        } else {
            binding.orderDetailRefundsInfo.hide()
        }

        // display refunds list in the payment info section, if available
        val formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)

        refunds.whenNotNullNorEmpty {
            binding.orderDetailPaymentInfo.showRefunds(order, it, formatCurrency)
        }.otherwise {
            binding.orderDetailPaymentInfo.showRefundTotal(
                show = order.isRefundAvailable,
                refundTotal = order.refundTotal,
                formatCurrencyForDisplay = formatCurrency
            )
        }
    }

    private fun showOrderProducts(products: List<OrderProduct>, currency: String) {
        products.whenNotNullNorEmpty {
            with(binding.orderDetailProductList) {
                updateProductItemsList(
                    orderProductItems = products,
                    productImageMap = productImageMap,
                    formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(currency),
                    productClickListener = this@OrderDetailFragment,
                    onProductMenuItemClicked = viewModel::onCreateShippingLabelButtonTapped,
                    onViewAddonsClick = viewModel::onViewOrderedAddonButtonTapped
                )
            }
        }.otherwise { binding.orderDetailProductList.hide() }
    }

    private fun showCustomAmounts(feeLine: LiveData<List<Order.FeeLine>>) {
        binding.orderDetailCustomAmount.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val feeLineState = feeLine.observeAsState(emptyList())
                if (feeLineState.value.isEmpty().not()) {
                    WooThemeWithBackground {
                        Column(
                            modifier = Modifier.padding(bottom = 1.dp)
                        ) {
                            Header(text = stringResource(id = R.string.order_detail_custom_amounts_header))
                            feeLineState.value.forEachIndexed { index, feeLine ->
                                CustomAmountCard(
                                    CustomAmountUI(
                                        name = feeLine.name ?: "",
                                        amount = CurrencyFormattedAmount(
                                            currencyFormatter.formatCurrency(feeLine.total)
                                        ),
                                        shouldShowDivider = index < feeLineState.value.size - 1,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showAddShipmentTracking(show: Boolean) {
        with(binding.orderDetailShipmentList) {
            isVisible = show
            showAddTrackingButton(show) { viewModel.onAddShipmentTrackingClicked() }
        }
    }

    private fun showShipmentTrackings(
        shipmentTrackings: List<OrderShipmentTracking>
    ) {
        binding.orderDetailShipmentList.updateShipmentTrackingList(
            shipmentTrackings = shipmentTrackings,
            dateUtils = dateUtils,
            onDeleteShipmentTrackingClicked = {
                viewModel.onDeleteShipmentTrackingClicked(it)
            }
        )
    }

    private fun showShippingLabels(shippingLabels: List<ShippingLabel>, currency: String) {
        shippingLabels.whenNotNullNorEmpty {
            with(binding.orderDetailShippingLabelList) {
                show()
                updateShippingLabels(
                    shippingLabels = shippingLabels,
                    productImageMap = productImageMap,
                    formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(currency),
                    productClickListener = this@OrderDetailFragment,
                    shippingLabelClickListener = object : OnShippingLabelClickListener {
                        override fun onRefundRequested(shippingLabel: ShippingLabel) {
                            viewModel.onRefundShippingLabelClick(shippingLabel.id)
                        }

                        override fun onPrintShippingLabelClicked(shippingLabel: ShippingLabel) {
                            viewModel.onPrintShippingLabelClicked(shippingLabel.id)
                        }

                        override fun onPrintCustomsFormClicked(shippingLabel: ShippingLabel) {
                            viewModel.onPrintCustomsFormClicked(shippingLabel)
                        }
                    }
                )
            }
        }.otherwise {
            binding.orderDetailShippingLabelList.hide()
        }
    }

    private fun displayShippingLabelsWIPCard(show: Boolean) {
        if (show && feedbackState != DISMISSED) {
            binding.orderDetailShippingLabelsWipCard.isVisible = true

            binding.orderDetailShippingLabelsWipCard.initView(
                getString(R.string.orderdetail_shipping_label_m2_wip_title),
                getString(R.string.orderdetail_shipping_label_m3_wip_message),
                onGiveFeedbackClick = { onGiveFeedbackClicked() },
                onDismissClick = { onDismissProductWIPNoticeCardClicked() }
            )
        } else {
            binding.orderDetailShippingLabelsWipCard.isVisible = false
        }
    }

    private fun onGiveFeedbackClicked() {
        val context = AnalyticsTracker.VALUE_SHIPPING_LABELS_M4_FEEDBACK

        AnalyticsTracker.track(
            FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to context,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
            )
        )
        registerFeedbackSetting(GIVEN)
        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.SHIPPING_LABELS)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissProductWIPNoticeCardClicked() {
        val context = AnalyticsTracker.VALUE_SHIPPING_LABELS_M4_FEEDBACK

        AnalyticsTracker.track(
            FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to context,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
            )
        )
        registerFeedbackSetting(DISMISSED)
        displayShippingLabelsWIPCard(false)
    }

    private fun registerFeedbackSetting(state: FeedbackState) {
        FeatureFeedbackSettings(
            SHIPPING_LABEL_M4,
            state
        ).registerItself(feedbackPrefs)
    }

    private fun displayUndoSnackbar(
        message: String,
        actionListener: View.OnClickListener,
        dismissCallback: Snackbar.Callback
    ) {
        undoSnackbar = uiMessageResolver.getUndoSnack(
            message = message,
            actionListener = actionListener
        ).also {
            it.addCallback(dismissCallback)
            it.show()
        }
    }

    private fun showEmptyView(show: Boolean) {
        if (show) {
            binding.orderDetailContainer.visibility = View.GONE
            binding.emptyView.show(WCEmptyView.EmptyViewType.ORDER_DETAILS)
        } else {
            binding.emptyView.hide()
        }
    }

    data class CustomAmountUI(
        val name: String,
        val amount: CurrencyFormattedAmount,
        val shouldShowDivider: Boolean,
    )

    @JvmInline
    value class CurrencyFormattedAmount(val amount: String)
}
