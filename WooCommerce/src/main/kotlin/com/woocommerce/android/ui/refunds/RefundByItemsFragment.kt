package com.woocommerce.android.ui.refunds

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.OpenUrl
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundAmountDialog
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_refund_by_items.*
import kotlinx.android.synthetic.main.refund_by_items_products.*
import java.math.BigDecimal
import javax.inject.Inject

class RefundByItemsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var imageMap: ProductImageMap

    private val viewModel: IssueRefundViewModel by navGraphViewModels(R.id.nav_graph_refunds) {
        viewModelFactory.get()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        return inflater.inflate(R.layout.fragment_refund_by_items, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
        setupObservers()
    }

    private fun initializeViews() {
        issueRefund_products.layoutManager = LinearLayoutManager(context)
        issueRefund_products.setHasFixedSize(true)
        issueRefund_products.isMotionEventSplittingEnabled = false

        issueRefund_selectButton.setOnClickListener {
            viewModel.onSelectButtonTapped()
        }

        issueRefund_btnNextFromItems.setOnClickListener {
            viewModel.onNextButtonTappedFromItems()
        }

        // TODO: Temporarily disabled, this will be used in a future release - do not remove
//        issueRefund_shippingSwitch.setOnCheckedChangeListener { _, isChecked ->
//            viewModel.onRefundItemsShippingSwitchChanged(isChecked)
//        }
//
//        issueRefund_productsTotal.setOnClickListener {
//            viewModel.onProductRefundAmountTapped()
//        }
    }

    private fun setupObservers() {
        viewModel.refundByItemsStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.currency?.takeIfNotEqualTo(old?.currency) {
                issueRefund_products.adapter = RefundProductListAdapter(
                        currencyFormatter.buildBigDecimalFormatter(new.currency),
                        imageMap,
                        false,
                        { uniqueId -> viewModel.onRefundQuantityTapped(uniqueId) }
                )
            }
            new.isNextButtonEnabled?.takeIfNotEqualTo(old?.isNextButtonEnabled) {
                issueRefund_btnNextFromItems.isEnabled = it
            }
            new.formattedProductsRefund?.takeIfNotEqualTo(old?.formattedProductsRefund) {
                issueRefund_productsTotal.text = it
            }
            new.shippingSubtotal?.takeIfNotEqualTo(old?.shippingSubtotal) {
                issueRefund_shippingTotal.text = it
            }
            new.feesTotal?.takeIfNotEqualTo(old?.feesTotal) {
                issueRefund_feesTotal.text = it
            }
            new.taxes?.takeIfNotEqualTo(old?.taxes) {
                issueRefund_taxesTotal.text = it
            }
            new.subtotal?.takeIfNotEqualTo(old?.subtotal) {
                issueRefund_subtotal.text = it
            }
            new.selectedItemsHeader?.takeIfNotEqualTo(old?.selectedItemsHeader) {
                issueRefund_selectedItems.text = it
            }
            new.selectButtonTitle?.takeIfNotEqualTo(old?.selectButtonTitle) {
                issueRefund_selectButton.text = it
            }
            new.isShippingRefundVisible?.takeIfNotEqualTo(old?.isShippingRefundVisible) { isVisible ->
                if (isVisible) {
                    issueRefund_shippingRefundGroup.show()
                } else {
                    issueRefund_shippingRefundGroup.hide()
                }
            }
            new.isShippingNoticeVisible?.takeIfNotEqualTo(old?.isShippingNoticeVisible) { isVisible ->
                if (isVisible) {
                    issueRefund_shippingRefundNotice.show()
                } else {
                    issueRefund_shippingRefundNotice.hide()
                }
            }
            new.isFeesVisible?.takeIfNotEqualTo(old?.isFeesVisible) { isVisible ->
                if (isVisible) {
                    issueRefund_feesGroup.show()
                    updateRefundNoticeView(getString(R.string.order_refunds_shipping_refund_notice_fees))
                } else {
                    issueRefund_feesGroup.hide()
                    updateRefundNoticeView(getString(R.string.order_refunds_shipping_refund_notice))
                }
            }
            // TODO: Temporarily disabled, this will be used in a future release - do not remove
//            new.isShippingRefundVisible?.takeIfNotEqualTo(old?.isShippingRefundVisible) { isVisible ->
//                if (isVisible) {
//                    issueRefund_shippingSection.expand()
//                } else {
//                    issueRefund_shippingSection.collapse()
//                }
//            }
        }

        viewModel.refundItems.observe(viewLifecycleOwner, Observer { list ->
            val adapter = issueRefund_products.adapter as RefundProductListAdapter
            adapter.update(list)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowNumberPicker -> {
                    val action = IssueRefundFragmentDirections.actionIssueRefundFragmentToRefundItemsPickerDialog(
                            getString(R.string.order_refunds_select_quantity),
                            event.refundItem.orderItem.uniqueId,
                            event.refundItem.maxQuantity,
                            event.refundItem.quantity
                    )
                    findNavController().navigateSafely(action)
                }
                is ShowRefundAmountDialog -> {
                    val action = IssueRefundFragmentDirections.actionIssueRefundFragmentToRefundAmountDialog(
                            getString(R.string.order_refunds_products_refund),
                            event.maxRefund,
                            event.refundAmount,
                            BigDecimal.ZERO,
                            event.message
                    )
                    findNavController().navigateSafely(action)
                }
                is OpenUrl -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                else -> event.isHandled = false
            }
        })
    }

    private fun updateRefundNoticeView(refundNoticeText: String) {
        val linkText = getString(R.string.order_refunds_store_admin_link_text)
        val noticeText = String.format(refundNoticeText, linkText)
        val spannable = SpannableString(noticeText)
        val span = WooClickableSpan { viewModel.onOpenStoreAdminLinkClicked() }
        span.useCustomStyle = false
        spannable.setSpan(
            span,
            (noticeText.length - linkText.length),
            noticeText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        issueRefund_shippingRefundNotice.setText(spannable, TextView.BufferType.SPANNABLE)
        issueRefund_shippingRefundNotice.movementMethod = LinkMovementMethod.getInstance()
    }
}
