package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.OrderNavigator
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelPaperSizeSelectorDialog.ShippingLabelPaperSize
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_print_shipping_label.*
import javax.inject.Inject

class PrintShippingLabelFragment : BaseFragment() {
    @Inject lateinit var navigator: OrderNavigator

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: PrintShippingLabelViewModel by viewModels { viewModelFactory }

    override fun getFragmentTitle() = getString(R.string.orderdetail_shipping_label_reprint)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_print_shipping_label, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        setupResultHandlers(viewModel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        shippingLabelPrint_paperSize.setClickListener { viewModel.onPaperSizeOptionsSelected() }
    }

    private fun setupObservers(viewModel: PrintShippingLabelViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.paperSize.takeIfNotEqualTo(old?.paperSize) {
                shippingLabelPrint_paperSize.setText(getString(it.stringResource))
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is OrderNavigationTarget -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        })
    }

    private fun setupResultHandlers(viewModel: PrintShippingLabelViewModel) {
        handleResult<ShippingLabelPaperSize>(ShippingLabelPaperSizeSelectorDialog.KEY_PAPER_SIZE_RESULT) {
            viewModel.onPaperSizeSelected(it)
        }
    }
}
