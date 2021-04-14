package com.woocommerce.android.ui.orders.details

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.FEATURE_FEEDBACK_BANNER
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_DETAIL_PRODUCT_TAPPED
import com.woocommerce.android.databinding.FragmentOrderDetailBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.extensions.whenNotNullNorEmpty
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.Feature.SHIPPING_LABELS_M1
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.DISMISSED
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.GIVEN
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.UNANSWERED
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.OrderNavigator
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShippingLabelsAdapter.OnShippingLabelClickListener
import com.woocommerce.android.ui.orders.notes.AddOrderNoteFragment
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelFragment
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRefundFragment
import com.woocommerce.android.ui.orders.tracking.AddOrderShipmentTrackingFragment
import com.woocommerce.android.ui.refunds.RefundSummaryFragment
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class OrderDetailFragment : BaseFragment(R.layout.fragment_order_detail), OrderProductActionListener {
    companion object {
        val TAG: String = OrderDetailFragment::class.java.simpleName
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: OrderDetailViewModel by viewModels { viewModelFactory }

    @Inject lateinit var navigator: OrderNavigator
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var productImageMap: ProductImageMap

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private val skeletonView = SkeletonView()
    private var undoSnackbar: Snackbar? = null
    private var screenTitle = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

    private val feedbackState
        get() = FeedbackPrefs.getFeatureFeedbackSettings(TAG)?.state ?: UNANSWERED

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        undoSnackbar?.dismiss()
        super.onStop()
    }

    override fun getFragmentTitle() = screenTitle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderDetailBinding.bind(view)

        setHasOptionsMenu(true)
        setupObservers(viewModel)
        setupResultHandlers(viewModel)

        binding.orderRefreshLayout.apply {
            scrollUpChild = binding.scrollView
            setOnRefreshListener { viewModel.onRefreshRequested() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun openOrderProductDetail(remoteProductId: Long) {
        AnalyticsTracker.track(ORDER_DETAIL_PRODUCT_TAPPED)
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }

    private fun setupObservers(viewModel: OrderDetailViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.order?.takeIfNotEqualTo(old?.order) { showOrderDetail(it) }
            new.orderStatus?.takeIfNotEqualTo(old?.orderStatus) { showOrderStatus(it) }
            new.isMarkOrderCompleteButtonVisible?.takeIfNotEqualTo(old?.isMarkOrderCompleteButtonVisible) {
                showMarkOrderCompleteButton(it)
            }
            new.isCreateShippingLabelButtonVisible?.takeIfNotEqualTo(old?.isCreateShippingLabelButtonVisible) {
                showShippingLabelButton(it)
            }
            new.isCreateShippingLabelBannerVisible.takeIfNotEqualTo(old?.isCreateShippingLabelBannerVisible) {
                displayShippingLabelsWIPCard(it, false)
            }
            new.isReprintShippingLabelBannerVisible.takeIfNotEqualTo(old?.isReprintShippingLabelBannerVisible) {
                displayShippingLabelsWIPCard(it, true)
            }
            new.isProductListVisible?.takeIfNotEqualTo(old?.isProductListVisible) {
                binding.orderDetailProductList.isVisible = it
            }
            new.toolbarTitle?.takeIfNotEqualTo(old?.toolbarTitle) { screenTitle = it }
            new.isOrderDetailSkeletonShown?.takeIfNotEqualTo(old?.isOrderDetailSkeletonShown) { showSkeleton(it) }
            new.isShipmentTrackingAvailable?.takeIfNotEqualTo(old?.isShipmentTrackingAvailable) {
                showAddShipmentTracking(it)
            }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                binding.orderRefreshLayout.isRefreshing = it
            }
            new.refreshedProductId?.takeIfNotEqualTo(old?.refreshedProductId) { refreshProduct(it) }
        }

        viewModel.orderNotes.observe(viewLifecycleOwner, Observer {
            showOrderNotes(it)
        })
        viewModel.orderRefunds.observe(viewLifecycleOwner, Observer {
            showOrderRefunds(it, viewModel.order)
        })
        viewModel.productList.observe(viewLifecycleOwner, Observer {
            showOrderProducts(it, viewModel.order.currency)
        })
        viewModel.shipmentTrackings.observe(viewLifecycleOwner, Observer {
            showShipmentTrackings(it)
        })
        viewModel.shippingLabels.observe(viewLifecycleOwner, Observer {
            showShippingLabels(it, viewModel.order.currency)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
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
                else -> event.isHandled = false
            }
        })
    }

    private fun setupResultHandlers(viewModel: OrderDetailViewModel) {
        handleDialogResult<String>(OrderStatusSelectorDialog.KEY_ORDER_STATUS_RESULT, R.id.orderDetailFragment) {
            viewModel.onOrderStatusChanged(it)
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
        handleNotice(RefundSummaryFragment.REFUND_ORDER_NOTICE_KEY) {
            viewModel.onOrderItemRefunded()
        }
        handleNotice(PrintShippingLabelFragment.KEY_LABEL_PURCHASED) {
            viewModel.onShippingLabelsPurchased()
        }
    }

    private fun showOrderDetail(order: Order) {
        binding.orderDetailOrderStatus.updateOrder(order)
        binding.orderDetailShippingMethodNotice.isVisible = order.multiShippingLinesAvailable
        binding.orderDetailCustomerInfo.updateCustomerInfo(
            order = order,
            isVirtualOrder = viewModel.hasVirtualProductsOnly()
        )
        binding.orderDetailPaymentInfo.updatePaymentInfo(
            order = order,
            formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency),
            onIssueRefundClickListener = { viewModel.onIssueOrderRefundClicked() },
            onCollectCardPresentPaymentClickListener = {
                if (FeatureFlag.CARD_READER.isEnabled()) {
                    val manager = (requireActivity().application as? WooCommerce)?.cardReaderManager
                    viewModel.onAcceptCardPresentPaymentClicked(cardReaderManager = manager)
                }
            }
        )
    }

    private fun showOrderStatus(orderStatus: OrderStatus) {
        binding.orderDetailOrderStatus.updateStatus(orderStatus) {
            viewModel.onEditOrderStatusSelected()
        }
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
        val refundsCount = refunds.sumBy { refund -> refund.items.sumBy { it.quantity } }
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

    private fun showOrderProducts(products: List<Order.Item>, currency: String) {
        products.whenNotNullNorEmpty {
            with(binding.orderDetailProductList) {
                updateProductList(
                    orderItems = products,
                    productImageMap = productImageMap,
                    formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(currency),
                    productClickListener = this@OrderDetailFragment
                )
            }
        }.otherwise { binding.orderDetailProductList.hide() }
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
        binding.orderDetailShipmentList.updateShipmentTrackingList(shipmentTrackings) {
            viewModel.onDeleteShipmentTrackingClicked(it)
        }
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
                    }
                )
            }
        }.otherwise {
            binding.orderDetailShippingLabelList.hide()
        }
    }

    private fun displayShippingLabelsWIPCard(show: Boolean, isReprintBanner: Boolean) {
        if (show && feedbackState != DISMISSED) {
            binding.orderDetailShippingLabelsWipCard.isVisible = true
            val (wipCardTitleId, wipCardMessageId) = if (isReprintBanner)
                R.string.orderdetail_shipping_label_wip_title to R.string.orderdetail_shipping_label_wip_message
            else
                R.string.orderdetail_shipping_label_m2_wip_title to R.string.orderdetail_shipping_label_m2_wip_message

            binding.orderDetailShippingLabelsWipCard.initView(
                getString(wipCardTitleId),
                getString(wipCardMessageId),
                onGiveFeedbackClick = { onGiveFeedbackClicked(isReprintBanner) },
                onDismissClick = { onDismissProductWIPNoticeCardClicked(isReprintBanner) }
            )
        } else binding.orderDetailShippingLabelsWipCard.isVisible = false
    }

    private fun onGiveFeedbackClicked(isM1: Boolean) {
        val context = if (isM1)
            AnalyticsTracker.VALUE_SHIPPING_LABELS_M1_FEEDBACK
        else
            AnalyticsTracker.VALUE_SHIPPING_LABELS_M2_FEEDBACK

        AnalyticsTracker.track(
            FEATURE_FEEDBACK_BANNER, mapOf(
            AnalyticsTracker.KEY_FEEDBACK_CONTEXT to context,
            AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
        ))
        registerFeedbackSetting(GIVEN)
        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.SHIPPING_LABELS)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissProductWIPNoticeCardClicked(isM1: Boolean) {
        val context = if (isM1)
            AnalyticsTracker.VALUE_SHIPPING_LABELS_M1_FEEDBACK
        else
            AnalyticsTracker.VALUE_SHIPPING_LABELS_M2_FEEDBACK

        AnalyticsTracker.track(
            FEATURE_FEEDBACK_BANNER, mapOf(
            AnalyticsTracker.KEY_FEEDBACK_CONTEXT to context,
            AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
        ))
        registerFeedbackSetting(DISMISSED)
        displayShippingLabelsWIPCard(false, isM1)
    }

    private fun registerFeedbackSetting(state: FeedbackState) {
        FeatureFeedbackSettings(SHIPPING_LABELS_M1.name, state)
            .run { FeedbackPrefs.setFeatureFeedbackSettings(TAG, this) }
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
}
