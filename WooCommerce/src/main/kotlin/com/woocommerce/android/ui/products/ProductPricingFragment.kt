package com.woocommerce.android.ui.products

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitPricing
import com.woocommerce.android.ui.products.ProductInventorySelectorDialog.ProductInventorySelectorDialogListener
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.widgets.WCMaterialOutlinedSpinnerView
import kotlinx.android.synthetic.main.fragment_product_pricing.*
import org.wordpress.android.util.ActivityUtils
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

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                // TODO: add track event for click
                ActivityUtils.hideKeyboard(activity)
                viewModel.onDoneButtonClicked(ExitPricing(shouldShowDiscardDialog = false))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitPricing -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })

        viewModel.initialisePricing()
    }

    private fun updateProductView(currency: String, decimals: Int, productData: ProductDetailViewState) {
        if (!isAdded) return

        val product = requireNotNull(productData.product)
        with(product_regular_price) {
            initialiseCurrencyEditText(currency, decimals, currencyFormatter)
            product.regularPrice?.let { setText(it) }
            getCurrencyEditText().value.observe(viewLifecycleOwner, Observer {
                viewModel.updateProductDraft(regularPrice = it)
            })
        }

        with(product_sale_price) {
            initialiseCurrencyEditText(currency, decimals, currencyFormatter)
            product.salePrice?.let { setText(it) }
            getCurrencyEditText().value.observe(viewLifecycleOwner, Observer {
                viewModel.updateProductDraft(salePrice = it)
            })
        }

        val scheduleSale = product.isSaleScheduled
        enableScheduleSale(scheduleSale)
        with(scheduleSale_switch) {
            isChecked = scheduleSale
            setOnCheckedChangeListener { _, isChecked ->
                enableScheduleSale(isChecked)
                viewModel.updateProductDraft(isSaleScheduled = isChecked)
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
                startDatePickerDialog = displayDatePickerDialog(scheduleSale_startDate, OnDateSetListener {
                    _, selectedYear, selectedMonth, dayOfMonth ->
                    val selectedDate = DateUtils.getOffsetGmtDate(
                            selectedYear, selectedMonth, dayOfMonth, gmtOffset
                    )
                    setText(selectedDate.formatToMMMddYYYY())
                    viewModel.updateProductDraft(dateOnSaleFrom = selectedDate.formatToYYYYmmDD())
                })
            }
        }

        with(scheduleSale_endDate) {
            val dateOnSaleTo = product.dateOnSaleToGmt?.let {
                DateUtils.offsetGmtDate(it, gmtOffset)
            } ?: currentDate

            setText(dateOnSaleTo.formatToMMMddYYYY())
            setClickListener {
                endDatePickerDialog = displayDatePickerDialog(scheduleSale_endDate, OnDateSetListener {
                    _, selectedYear, selectedMonth, dayOfMonth ->
                    val selectedDate = DateUtils.getOffsetGmtDate(
                            selectedYear, selectedMonth, dayOfMonth, gmtOffset
                    )
                    setText(selectedDate.formatToMMMddYYYY())
                    viewModel.updateProductDraft(dateOnSaleTo = selectedDate.formatToYYYYmmDD())
                })
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
        spinnerEditText: WCMaterialOutlinedSpinnerView,
        dateSetListener: OnDateSetListener
    ): DatePickerDialog {
        val dateString = DateUtils.formatToYYYYmmDD(spinnerEditText.getText())
        val (year, month, day) = dateString.split("-")
        val datePicker = DatePickerDialog(
                requireActivity(), dateSetListener, year.toInt(), month.toInt() - 1, day.toInt()
        )

        datePicker.show()
        return datePicker
    }

    override fun onProductInventoryItemSelected(resultCode: Int, selectedItem: String?) {
        when (resultCode) {
            RequestCodes.PRODUCT_TAX_STATUS -> {
                selectedItem?.let {
                    product_tax_status.setText(getString(ProductTaxStatus.toStringResource(it)))
                    viewModel.updateProductDraft(taxStatus = ProductTaxStatus.fromString(it))
                }
            }
            RequestCodes.PRODUCT_TAX_CLASS -> {
                selectedItem?.let { selectedTaxClass ->
                    // Fetch the display name of the selected tax class slug
                    val selectedProductTaxClass = viewModel.getSelectedTaxClass(selectedTaxClass)
                    selectedProductTaxClass?.let {
                        product_tax_class.setText(it.name)
                        viewModel.updateProductDraft(taxClass = it.slug)
                    }
                }
            }
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitPricing())
    }
}
