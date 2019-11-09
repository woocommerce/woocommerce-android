package com.woocommerce.android.ui.refunds

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.NumberPickerDialog
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_refund_by_items.*
import javax.inject.Inject

class RefundByItemsFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var imageMap: ProductImageMap

    companion object {
        const val PRODUCT_ID_KEY = "PRODUCT_ID_KEY"
        const val REFUND_ITEM_QUANTITY_REQUEST_CODE = 12345
    }

    private val viewModel: IssueRefundViewModel by activityViewModels { viewModelFactory }

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

        issueRefund_selectAllButton.setOnClickListener {
            viewModel.onSelectAllButtonTapped()
        }

        issueRefund_btnNextFromItems.setOnClickListener {
            viewModel.onNextButtonTappedFromItems()
        }
    }

    private fun setupObservers() {
        viewModel.refundByItemsStateLiveData.observe(this) { old, new ->
            new.currency?.takeIfNotEqualTo(old?.currency) {
                issueRefund_products.adapter = RefundProductListAdapter(
                        currencyFormatter.buildBigDecimalFormatter(new.currency),
                        { productId -> viewModel.onRefundQuantityTapped(productId) },
                        imageMap
                )
            }
            new.items?.takeIfNotEqualTo(old?.items) { list ->
                val adapter = issueRefund_products.adapter as RefundProductListAdapter
                adapter.update(list)

                val selectedItems = list.sumBy { it.quantity }
                issueRefund_btnNextFromItems.isEnabled = selectedItems > 0

                val selectedItemsHeader = getString(R.string.order_refunds_items_selected, selectedItems)
                issueRefund_selectedItems.text = selectedItemsHeader
            }
        }

        viewModel.event.observe(this, Observer { event ->
            when (event) {
                is ShowNumberPicker -> {
                    val args = Bundle()
                    args.putString(NumberPickerDialog.TITLE_KEY, "Items")
                    args.putLong(PRODUCT_ID_KEY, event.refundItem.product.productId)
                    args.putInt(NumberPickerDialog.MAX_VALUE_KEY, event.refundItem.product.quantity.toInt())
                    args.putInt(NumberPickerDialog.CUR_VALUE_KEY, event.refundItem.quantity)
                    val dialog = NumberPickerDialog.createInstance(args, NumberPicker.Formatter {
                        it.toString()
                    })
                    dialog.setTargetFragment(this, REFUND_ITEM_QUANTITY_REQUEST_CODE)
                    dialog.show(requireFragmentManager(), "item-picker")
                }
                else -> event.isHandled = false
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REFUND_ITEM_QUANTITY_REQUEST_CODE -> {
                val productId = data?.getLongExtra(PRODUCT_ID_KEY, 0)
                val quantity = data?.getIntExtra(NumberPickerDialog.CUR_VALUE_KEY, 0)
                if (productId != null && quantity != null) {
                    viewModel.onRefundQuantityChanged(productId, quantity)
                }
            }
        }
    }
}
