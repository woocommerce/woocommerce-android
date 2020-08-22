package com.woocommerce.android.ui.products

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.offsetGmtDate
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.ui.products.ProductItemSelectorDialog.ProductItemSelectorDialogListener
import com.woocommerce.android.ui.products.ProductPricingViewModel.PricingData
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.WCMaterialOutlinedSpinnerView
import kotlinx.android.synthetic.main.fragment_product_pricing.*
import java.util.Date

class ProductPricingFragment
    : BaseProductEditorFragment(R.layout.fragment_product_pricing), ProductItemSelectorDialogListener {
    private val viewModel: ProductPricingViewModel by viewModels { viewModelFactory }

    override val isDoneButtonVisible: Boolean
        get() = viewModel.viewStateData.liveData.value?.isDoneButtonVisible ?: false
    override val isDoneButtonEnabled: Boolean
        get() = viewModel.viewStateData.liveData.value?.isDoneButtonEnabled ?: false
    override val lastEvent: Event?
        get() = viewModel.event.value

    private var productTaxStatusSelectorDialog: ProductItemSelectorDialog? = null
    private var productTaxClassSelectorDialog: ProductItemSelectorDialog? = null

    private var startDatePickerDialog: DatePickerDialog? = null
    private var endDatePickerDialog: DatePickerDialog? = null

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

    override fun getFragmentTitle() = getString(R.string.product_price)

    private fun setupObservers(viewModel: ProductPricingViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.currency?.takeIfNotEqualTo(old?.currency) {
                setupViews(new.currency, new.decimals, viewModel.pricingData)
            }
            new.taxClassList?.takeIfNotEqualTo(old?.taxClassList) {
                updateProductTaxClassList(it, viewModel.pricingData)
            }
            new.pricingData.saleStartDate?.takeIfNotEqualTo(old?.pricingData?.saleStartDate) {
                scheduleSale_startDate.setText(it.formatToMMMddYYYY())
            }
            new.pricingData.saleEndDate?.takeIfNotEqualTo(old?.pricingData?.saleEndDate) {
                scheduleSale_endDate.setText(it.formatToMMMddYYYY())
            }
            new.isRemoveEndDateButtonVisible.takeIfNotEqualTo(old?.isRemoveEndDateButtonVisible) { isVisible ->
                scheduleSale_RemoveEndDateButton.visibility = if (isVisible) View.VISIBLE else {
                    scheduleSale_endDate.setText("")
                    View.GONE
                }
            }
            new.isDoneButtonVisible?.takeIfNotEqualTo(old?.isDoneButtonVisible) { isVisible ->
                doneButton?.isVisible = isVisible
            }
            new.isDoneButtonEnabled.takeIfNotEqualTo(old?.isDoneButtonEnabled) { isEnabled ->
                doneButton?.isEnabled = isEnabled
            }
            new.isTaxSectionVisible?.takeIfNotEqualTo(old?.isTaxSectionVisible) { isVisible ->
                if (isVisible) {
                    product_tax_section?.show()
                } else {
                    product_tax_section?.hide()
                }
            }
            new.salePriceErrorMessage?.takeIfNotEqualTo(old?.salePriceErrorMessage) { displaySalePriceError(it) }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(KEY_PRICING_DIALOG_RESULT, event.data)
                is Exit -> findNavController().navigateUp()
                is ShowDiscardDialog -> CustomDiscardDialog.showDiscardDialog(
                    requireActivity(),
                    event.positiveBtnAction,
                    event.negativeBtnAction,
                    event.messageId
                )
                else -> event.isHandled = false
            }
        })
    }

    private fun setupViews(currency: String, decimals: Int, pricingData: PricingData) {
        if (!isAdded) return

        with(product_regular_price) {
            initView(currency, decimals, currencyFormatter)
            pricingData.regularPrice?.let { setValue(it) }
            getCurrencyEditText().value.observe(viewLifecycleOwner, Observer {
                viewModel.onRegularPriceEntered(it)
            })
        }

        with(product_sale_price) {
            initView(currency, decimals, currencyFormatter)
            pricingData.salePrice?.let { setValue(it) }
            getCurrencyEditText().value.observe(viewLifecycleOwner, Observer {
                viewModel.onSalePriceEntered(it)
            })
        }

        val scheduleSale = pricingData.isSaleScheduled == true
        val gmtOffset = viewModel.parameters.gmtOffset

        enableScheduleSale(scheduleSale)
        with(scheduleSale_switch) {
            isChecked = scheduleSale
            setOnCheckedChangeListener { _, isChecked ->
                enableScheduleSale(isChecked)
                viewModel.onScheduledSaleChanged(isChecked)
            }
        }

        updateSaleStartDate(pricingData.saleStartDate, pricingData.saleEndDate, viewModel.parameters.gmtOffset)
        with(scheduleSale_startDate) {
            setClickListener {
                startDatePickerDialog = displayDatePickerDialog(scheduleSale_startDate, OnDateSetListener {
                    _, selectedYear, selectedMonth, dayOfMonth ->
                    val selectedDate = DateUtils.localDateToGmt(
                            selectedYear, selectedMonth, dayOfMonth, gmtOffset, true
                    )

                    viewModel.onDataChanged(saleStartDate = selectedDate)
                })
            }
        }

        updateSaleEndDate(pricingData.saleEndDate, viewModel.parameters.gmtOffset)
        with(scheduleSale_endDate) {
            setClickListener {
                endDatePickerDialog = displayDatePickerDialog(scheduleSale_endDate, OnDateSetListener {
                    _, selectedYear, selectedMonth, dayOfMonth ->
                    val selectedDate = DateUtils.localDateToGmt(
                            selectedYear, selectedMonth, dayOfMonth, gmtOffset, false
                    )

                    viewModel.onDataChanged(saleEndDate = selectedDate)
                })
            }
        }

        with(scheduleSale_RemoveEndDateButton) {
            setOnClickListener {
                viewModel.onRemoveEndDateClicked()
            }
        }

        pricingData.taxStatus?.let { status ->
            with(product_tax_status) {
                setText(ProductTaxStatus.taxStatusToDisplayString(requireContext(), status))
                setClickListener {
                    productTaxStatusSelectorDialog = ProductItemSelectorDialog.newInstance(
                        this@ProductPricingFragment, RequestCodes.PRODUCT_TAX_STATUS,
                        getString(R.string.product_tax_status), ProductTaxStatus.toMap(requireContext()),
                        getText()
                    ).also { it.show(parentFragmentManager, ProductItemSelectorDialog.TAG) }
                }
            }
        }
    }

    override fun onDoneButtonClicked() {
        viewModel.onDoneButtonClicked()
    }

    override fun onExit() {
        viewModel.onExit()
    }

    /**
     * Method to update the start date of a sale using the [offset]
     *
     * If the [selectedStartDate] is empty or null, then the default is set to the current date,
     * only if the [endDate] > the current date.
     *
     * The [viewModel] is only updated if the [selectedStartDate] is not null. This is to prevent
     * the discard dialog from being displayed when there have been no user initiated changes made
     * to the screen.
     */
    private fun updateSaleStartDate(selectedStartDate: Date?, endDate: Date?, offset: Float) {
        val currentDate = Date()
        val date = selectedStartDate
            ?: if (endDate?.after(currentDate) == true) {
                currentDate
            } else null

        date?.let { scheduleSale_startDate.setText(formatSaleDateForDisplay(it, offset)) }
        selectedStartDate?.let { viewModel.onDataChanged(saleStartDate = it) }
    }

    private fun updateSaleEndDate(selectedDate: Date?, offset: Float) {
        // The end sale date is optional => null is a valid value
        if (selectedDate != null) {
            scheduleSale_endDate.setText(formatSaleDateForDisplay(selectedDate, offset))
        } else {
            scheduleSale_endDate.setText("")
        }
        viewModel.onDataChanged(saleEndDate = selectedDate)
    }

    private fun updateProductTaxClassList(taxClassList: List<TaxClass>?, pricingData: PricingData) {
        val taxClass = viewModel.getTaxClassBySlug(pricingData.taxClass ?: Product.TAX_CLASS_DEFAULT)
        val name = taxClass?.name
        if (!isAdded || name == null) return

        product_tax_class.setText(name)
        taxClassList?.let { taxClasses ->
            product_tax_class.setClickListener {
                productTaxClassSelectorDialog = ProductItemSelectorDialog.newInstance(
                    this@ProductPricingFragment,
                    RequestCodes.PRODUCT_TAX_CLASS,
                    getString(R.string.product_tax_class),
                    taxClasses.map { it.slug to it.name }.toMap(),
                    product_tax_class.getText()
                ).also { it.show(parentFragmentManager, ProductItemSelectorDialog.TAG) }
            }
        }
    }

    private fun displaySalePriceError(messageId: Int) {
        if (messageId != 0) {
            product_sale_price.error = getString(messageId)
        } else {
            product_sale_price.error = null
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
        val dateString = if (spinnerEditText.getText().isNotBlank())
            DateUtils.formatToYYYYmmDD(spinnerEditText.getText())
        else
            DateUtils.formatToYYYYmmDD(Date().formatToMMMddYYYY())
        val (year, month, day) = dateString.split("-")
        val datePicker = DatePickerDialog(
                requireActivity(), dateSetListener, year.toInt(), month.toInt() - 1, day.toInt()
        )

        datePicker.show()
        return datePicker
    }

    /**
     * Parses the given [date] and applies the passed [gmtOffset] and
     * formats this formatted date to MMM dd, YYYY format
     *
     * If given [date] is null, current date is used
     */
    private fun formatSaleDateForDisplay(date: Date?, gmtOffset: Float): String {
        val currentDate = DateUtils.offsetGmtDate(Date(), gmtOffset)
        val dateOnSaleFrom = date?.offsetGmtDate(gmtOffset) ?: currentDate
        return dateOnSaleFrom.formatToMMMddYYYY()
    }

    override fun onProductItemSelected(resultCode: Int, selectedItem: String?) {
        when (resultCode) {
            RequestCodes.PRODUCT_TAX_STATUS -> {
                selectedItem?.let {
                    product_tax_status.setText(getString(ProductTaxStatus.toStringResource(it)))
                    viewModel.onDataChanged(taxStatus = ProductTaxStatus.fromString(it))
                }
            }
            RequestCodes.PRODUCT_TAX_CLASS -> {
                selectedItem?.let { selectedTaxClass ->
                    // Fetch the display name of the selected tax class slug
                    val selectedProductTaxClass = viewModel.getTaxClassBySlug(selectedTaxClass)
                    selectedProductTaxClass?.let {
                        product_tax_class.setText(it.name)
                        viewModel.onDataChanged(taxClass = it.slug)
                    }
                }
            }
        }
    }
}
