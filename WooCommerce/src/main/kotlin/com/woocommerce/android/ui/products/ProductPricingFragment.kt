package com.woocommerce.android.ui.products

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.ui.products.ProductInventorySelectorDialog.ProductInventorySelectorDialogListener
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.widgets.WCMaterialOutlinedSpinnerView
import kotlinx.android.synthetic.main.fragment_product_pricing.*
import java.util.Date
import javax.inject.Inject

class ProductPricingFragment : BaseProductFragment(), ProductInventorySelectorDialogListener {
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private var productTaxStatusSelectorDialog: ProductInventorySelectorDialog? = null
    private var productTaxClassSelectorDialog: ProductInventorySelectorDialog? = null

    private var startDatePickerDialog: DatePickerDialog? = null
    private var endDatePickerDialog: DatePickerDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_pricing, container, false)
    }

    override fun onPause() {
        super.onPause()
        productTaxStatusSelectorDialog?.dismiss()
        productTaxStatusSelectorDialog = null

        productTaxClassSelectorDialog?.dismiss()
        productTaxClassSelectorDialog = null

        startDatePickerDialog?.dismiss()
        startDatePickerDialog = null

        endDatePickerDialog?.dismiss()
        endDatePickerDialog = null
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
            new.taxClassList?.takeIfNotEqualTo(old?.taxClassList) {
                updateProductTaxClassList(it)
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
            // TODO: update viewmodel
        }

        with(product_sale_price) {
            initialiseCurrencyEditText(currency, decimals, currencyFormatter)
            product.salePrice?.let { setText(it) }
            // TODO: update viewmodel
        }

        val scheduleSale = product.dateOnSaleFromGmt != null || product.dateOnSaleToGmt != null
        enableScheduleSale(scheduleSale)
        with(scheduleSale_switch) {
            isChecked = scheduleSale
            setOnCheckedChangeListener { _, isChecked ->
                enableScheduleSale(isChecked)
                // TODO: update viewmodel
            }
        }

        val gmtOffset = productData.gmtOffset
        val currentDate = DateUtils.offsetGmtDate(Date(), gmtOffset)
        with(scheduleSale_startDate) {
            val dateOnSaleFrom = product.dateOnSaleFromGmt?.let {
                DateUtils.offsetGmtDate(it, gmtOffset)
            } ?: currentDate

            setText(dateOnSaleFrom.formatToMMMddYYYY())
            setClickListener {
                startDatePickerDialog = displayDatePickerDialog(dateOnSaleFrom, scheduleSale_startDate)
                // TODO: update viewmodel
            }
        }

        with(scheduleSale_endDate) {
            val dateOnSaleTo = product.dateOnSaleToGmt?.let {
                DateUtils.offsetGmtDate(it, gmtOffset)
            } ?: currentDate

            setText(dateOnSaleTo.formatToMMMddYYYY())
            setClickListener {
                endDatePickerDialog = displayDatePickerDialog(dateOnSaleTo, scheduleSale_endDate)
                // TODO: update viewmodel
            }
        }

        with(product_tax_status) {
            setText(ProductTaxStatus.taxStatusToDisplayString(requireContext(), product.taxStatus))
            setClickListener {
                productTaxStatusSelectorDialog = ProductInventorySelectorDialog.newInstance(
                        this@ProductPricingFragment, RequestCodes.PRODUCT_TAX_STATUS,
                        getString(R.string.product_tax_status), ProductTaxStatus.toMap(requireContext()),
                        getText()
                ).also { it.show(parentFragmentManager, ProductInventorySelectorDialog.TAG) }
            }
        }

        product_tax_class.setText(product.taxClass)
    }

    private fun updateProductTaxClassList(taxClassList: List<TaxClass>?) {
        taxClassList?.let { taxClasses ->
            product_tax_class.setClickListener {
                productTaxClassSelectorDialog = ProductInventorySelectorDialog.newInstance(
                        this@ProductPricingFragment, RequestCodes.PRODUCT_TAX_CLASS,
                        getString(R.string.product_tax_class), taxClasses.map { it.slug to it.name }.toMap(),
                        product_tax_class.getText()
                ).also { it.show(parentFragmentManager, ProductInventorySelectorDialog.TAG) }
            }
        }
    }

    private fun enableScheduleSale(scheduleSale: Boolean) {
        if (scheduleSale) {
            scheduleSale_morePanel.expand()
        } else {
            scheduleSale_morePanel.collapse()
        }
    }

    private fun displayDatePickerDialog(
        date: Date,
        spinnerEditText: WCMaterialOutlinedSpinnerView
    ): DatePickerDialog {
        val (year, month, day) = date.formatToYYYYmmDD().split("-")
        val datePicker = DatePickerDialog(requireActivity(),
                DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, dayOfMonth ->
                    spinnerEditText.setText(DateUtils.formatToYYYYmmDD(selectedYear, selectedMonth, dayOfMonth))
                }, year.toInt(), month.toInt() - 1, day.toInt())

        datePicker.show()
        return datePicker
    }

    override fun onProductInventoryItemSelected(resultCode: Int, selectedItem: String?) {
        when (resultCode) {
            RequestCodes.PRODUCT_TAX_STATUS -> {
                selectedItem?.let {
                    product_tax_status.setText(getString(ProductTaxStatus.toStringResource(it)))
                    // TODO: update viewmodel
                }
            }
            RequestCodes.PRODUCT_TAX_CLASS -> {
                selectedItem?.let { selectedTaxClass ->
                    // Fetch the display name of the selected tax class slug
                    val selectedProductTaxClass = viewModel.getSelectedTaxClass(selectedTaxClass)
                    selectedProductTaxClass?.let { product_tax_class.setText(it.name) }
                    // TODO: update viewmodel
                }
            }
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return true
    }
}
