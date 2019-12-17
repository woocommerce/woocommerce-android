package com.woocommerce.android.ui.refunds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundAmountDialog
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_refund_by_items.*
import kotlinx.android.synthetic.main.refund_by_items_products.*
import java.math.BigDecimal
import javax.inject.Inject

class RefundByItemsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var imageMap: ProductImageMap

    private val viewModel: IssueRefundViewModel by navGraphViewModels(R.id.nav_graph_refunds) { viewModelFactory }

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

        issueRefund_selectButton.setOnClickListener {
            viewModel.onSelectButtonTapped()
        }

        issueRefund_btnNextFromItems.setOnClickListener {
            viewModel.onNextButtonTappedFromItems()
        }

        issueRefund_shippingSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onRefundItemsShippingSwitchChanged(isChecked)
        }
        // TODO: Temporarily disabled, this will be used in a future release - do not remove
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
                else -> event.isHandled = false
            }
        })
    }
}
