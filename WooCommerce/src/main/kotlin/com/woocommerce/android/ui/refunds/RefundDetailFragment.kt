package com.woocommerce.android.ui.refunds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import kotlinx.android.synthetic.main.fragment_refund_detail.*
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.refund_by_items_products.*
import javax.inject.Inject

class RefundDetailFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var imageMap: ProductImageMap

    private val viewModel: RefundDetailViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        return inflater.inflate(R.layout.fragment_refund_detail, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
        setupObservers(viewModel)
    }

    private fun initializeViews() {
        issueRefund_products.layoutManager = LinearLayoutManager(context)
        issueRefund_products.setHasFixedSize(true)
    }

    private fun setupObservers(viewModel: RefundDetailViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.screenTitle?.takeIfNotEqualTo(old?.screenTitle) {
                activity?.title = new.screenTitle
            }
            new.refundAmount?.takeIfNotEqualTo(old?.refundAmount) {
                refundDetail_refundAmount.text = new.refundAmount
                issueRefund_productsTotal.text = new.refundAmount
            }
            new.subtotal?.takeIfNotEqualTo(old?.subtotal) {
                issueRefund_subtotal.text = new.subtotal
            }
            new.taxes?.takeIfNotEqualTo(old?.taxes) {
                issueRefund_taxesTotal.text = new.taxes
            }
            new.refundMethod?.takeIfNotEqualTo(old?.refundMethod) {
                refundDetail_refundMethod.text = new.refundMethod
            }
            new.currency?.takeIfNotEqualTo(old?.currency) {
                issueRefund_products.adapter = RefundProductListAdapter(
                        currencyFormatter.buildBigDecimalFormatter(new.currency),
                        imageMap,
                        isProductDetailList = true,
                        onItemClicked = { uniqueId ->
                            (activity as? MainNavigationRouter)?.showProductDetail(uniqueId, enableTrash = false)
                        }
                )
            }
            new.areItemsVisible?.takeIfNotEqualTo(old?.areItemsVisible) { isVisible ->
                if (isVisible) {
                    refundDetail_refundItems.show()
                } else {
                    refundDetail_refundItems.hide()
                }
            }
            new.areDetailsVisible?.takeIfNotEqualTo(old?.areDetailsVisible) { isVisible ->
                if (isVisible) {
                    refundDetail_detailsCard.show()
                    refundDetail_reasonCard.show()
                    issueRefund_totalsGroup.show()
                } else {
                    refundDetail_detailsCard.hide()
                    refundDetail_reasonCard.hide()
                    issueRefund_totalsGroup.hide()
                }
            }

            if (new.refundReason.isNullOrEmpty()) {
                refundDetail_reasonCard.hide()
            } else {
                refundDetail_reasonCard.show()
                refundDetail_refundReason.text = new.refundReason
            }
        }

        viewModel.refundItems.observe(viewLifecycleOwner, Observer { list ->
            val adapter = issueRefund_products.adapter as RefundProductListAdapter
            adapter.update(list)
        })
    }
}
