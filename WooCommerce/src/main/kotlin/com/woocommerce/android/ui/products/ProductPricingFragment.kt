package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import kotlinx.android.synthetic.main.fragment_product_pricing.*
import java.util.Date
import javax.inject.Inject

class ProductPricingFragment : BaseProductFragment() {
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_pricing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productPricingViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.currency?.takeIfNotEqualTo(old?.currency) {
                updateProductView(new.currency, new.decimals, viewModel.getProduct())
            }
        }

        viewModel.initialisePricing()
    }

    private fun updateProductView(currency: String, decimals: Int, productData: ProductDetailViewState) {
        if (!isAdded) return

        val product = requireNotNull(productData.product)
        with(product_regular_price) {
            initialiseCurrencyEditText(currency, decimals, currencyFormatter)
            product.price?.let { setText(it) }
        }

        with(product_sale_price) {
            initialiseCurrencyEditText(currency, decimals, currencyFormatter)
            product.salePrice?.let { setText(it) }
        }

        val scheduleSale = product.dateOnSaleFromGmt != null || product.dateOnSaleToGmt != null
        enableScheduleSale(scheduleSale)
        with(scheduleSale_switch) {
            isChecked = scheduleSale
            setOnCheckedChangeListener { _, isChecked ->
                enableScheduleSale(isChecked)
            }
        }

        val gmtOffset = productData.gmtOffset
        val currentDate = DateUtils.offsetGmtDate(Date(), gmtOffset)
        with(scheduleSale_startDate) {
            val dateOnSaleFrom = product.dateOnSaleFromGmt?.let {
                DateUtils.offsetGmtDate(it, gmtOffset)
            } ?: currentDate

            setText(dateOnSaleFrom.formatToMMMddYYYY())
        }

        with(scheduleSale_endDate) {
            val dateOnSaleTo = product.dateOnSaleToGmt?.let {
                DateUtils.offsetGmtDate(it, gmtOffset)
            } ?: currentDate

            setText(dateOnSaleTo.formatToMMMddYYYY())
        }

        // TODO: update with correct product info in the next commit
        with(product_tax_status) {
            setText("Taxable")
        }

        // TODO: update with correct product info in the next commit
        with(product_tax_class) {
            setText("Standard")
        }
    }

    private fun enableScheduleSale(scheduleSale: Boolean) {
        if (scheduleSale) {
            scheduleSale_morePanel.expand()
        } else {
            scheduleSale_morePanel.collapse()
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return true
    }
}
