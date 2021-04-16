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
import com.woocommerce.android.databinding.FragmentProductPricingBinding
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.offsetGmtDate
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.ui.products.ProductItemSelectorDialog.ProductItemSelectorDialogListener
import com.woocommerce.android.ui.products.ProductPricingViewModel.PricingData
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.WCMaterialOutlinedSpinnerView
import java.util.Date
import javax.inject.Inject

class ProductPricingFragment
    : BaseProductEditorFragment(R.layout.fragment_product_pricing), ProductItemSelectorDialogListener {
    private val viewModel: ProductPricingViewModel by viewModels { viewModelFactory.get() }

    override val lastEvent: Event?
        get() = viewModel.event.value

    private var productTaxStatusSelectorDialog: ProductItemSelectorDialog? = null
    private var productTaxClassSelectorDialog: ProductItemSelectorDialog? = null

    private var startDatePickerDialog: DatePickerDialog? = null
    private var endDatePickerDialog: DatePickerDialog? = null

    private var _binding: FragmentProductPricingBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var dateUtils: DateUtils

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
        _binding = FragmentProductPricingBinding.bind(view)
        setupObservers(viewModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                binding.scheduleSaleStartDate.setText(it.formatToMMMddYYYY())
            }
            new.pricingData.saleEndDate?.takeIfNotEqualTo(old?.pricingData?.saleEndDate) {
                binding.scheduleSaleEndDate.setText(it.formatToMMMddYYYY())
            }
            new.isRemoveEndDateButtonVisible.takeIfNotEqualTo(old?.isRemoveEndDateButtonVisible) { isVisible ->
                binding.scheduleSaleRemoveEndDateButton.visibility = if (isVisible) View.VISIBLE else {
                    binding.scheduleSaleEndDate.setText("")
                    View.GONE
                }
            }
            new.isTaxSectionVisible?.takeIfNotEqualTo(old?.isTaxSectionVisible) { isVisible ->
                if (isVisible) {
                    binding.productTaxSection.show()
                } else {
                    binding.productTaxSection.hide()
                }
            }
            new.salePriceErrorMessage?.takeIfNotEqualTo(old?.salePriceErrorMessage) { displaySalePriceError(it) }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(KEY_PRICING_DIALOG_RESULT, event.data)
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
    }

    private fun setupViews(currency: String, decimals: Int, pricingData: PricingData) {
        if (!isAdded) return

        with(binding.productRegularPrice) {
            initView(currency, decimals, currencyFormatter)
            pricingData.regularPrice?.let { setValue(it) }
            getCurrencyEditText().value.observe(viewLifecycleOwner, Observer {
                viewModel.onRegularPriceEntered(it)
            })
        }

        with(binding.productSalePrice) {
            initView(currency, decimals, currencyFormatter)
            pricingData.salePrice?.let { setValue(it) }
            getCurrencyEditText().value.observe(viewLifecycleOwner, Observer {
                viewModel.onSalePriceEntered(it)
            })
        }

        val scheduleSale = pricingData.isSaleScheduled == true
        val gmtOffset = viewModel.parameters.gmtOffset

        enableScheduleSale(scheduleSale)
        with(binding.scheduleSaleSwitch) {
            isChecked = scheduleSale
            setOnCheckedChangeListener { _, isChecked ->
                enableScheduleSale(isChecked)
                viewModel.onScheduledSaleChanged(isChecked)
            }
        }

        updateSaleStartDate(pricingData.saleStartDate, pricingData.saleEndDate, viewModel.parameters.gmtOffset)
        with(binding.scheduleSaleStartDate) {
            setClickListener {
                startDatePickerDialog = displayDatePickerDialog(binding.scheduleSaleStartDate, OnDateSetListener {
                    _, selectedYear, selectedMonth, dayOfMonth ->
                    val selectedDate = dateUtils.localDateToGmt(
                            selectedYear, selectedMonth, dayOfMonth, gmtOffset, true
                    )

                    viewModel.onDataChanged(saleStartDate = selectedDate)
                })
            }
        }

        updateSaleEndDate(pricingData.saleEndDate, viewModel.parameters.gmtOffset)
        with(binding.scheduleSaleEndDate) {
            setClickListener {
                endDatePickerDialog = displayDatePickerDialog(binding.scheduleSaleEndDate, OnDateSetListener {
                    _, selectedYear, selectedMonth, dayOfMonth ->
                    val selectedDate = dateUtils.localDateToGmt(
                            selectedYear, selectedMonth, dayOfMonth, gmtOffset, false
                    )

                    viewModel.onDataChanged(saleEndDate = selectedDate)
                })
            }
        }

        with(binding.scheduleSaleRemoveEndDateButton) {
            setOnClickListener {
                viewModel.onRemoveEndDateClicked()
            }
        }

        pricingData.taxStatus?.let { status ->
            with(binding.productTaxStatus) {
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

        date?.let { binding.scheduleSaleStartDate.setText(formatSaleDateForDisplay(it, offset)) }
        selectedStartDate?.let { viewModel.onDataChanged(saleStartDate = it) }
    }

    private fun updateSaleEndDate(selectedDate: Date?, offset: Float) {
        // The end sale date is optional => null is a valid value
        if (selectedDate != null) {
            binding.scheduleSaleEndDate.setText(formatSaleDateForDisplay(selectedDate, offset))
        } else {
            binding.scheduleSaleEndDate.setText("")
        }
        viewModel.onDataChanged(saleEndDate = selectedDate)
    }

    private fun updateProductTaxClassList(taxClassList: List<TaxClass>?, pricingData: PricingData) {
        val taxClass = viewModel.getTaxClassBySlug(pricingData.taxClass ?: Product.TAX_CLASS_DEFAULT)
        val name = taxClass?.name
        if (!isAdded || name == null) return

        binding.productTaxClass.setText(name)
        taxClassList?.let { taxClasses ->
            binding.productTaxClass.setClickListener {
                productTaxClassSelectorDialog = ProductItemSelectorDialog.newInstance(
                    this@ProductPricingFragment,
                    RequestCodes.PRODUCT_TAX_CLASS,
                    getString(R.string.product_tax_class),
                    taxClasses.map { it.slug to it.name }.toMap(),
                    binding.productTaxClass.getText()
                ).also { it.show(parentFragmentManager, ProductItemSelectorDialog.TAG) }
            }
        }
    }

    private fun displaySalePriceError(messageId: Int) {
        if (messageId != 0) {
            binding.productSalePrice.error = getString(messageId)
        } else {
            binding.productSalePrice.error = null
        }
    }

    private fun enableScheduleSale(scheduleSale: Boolean) {
        if (scheduleSale) {
            binding.scheduleSaleMorePanel.expand()
        } else {
            binding.scheduleSaleMorePanel.collapse()
        }
    }

    private fun displayDatePickerDialog(
        spinnerEditText: WCMaterialOutlinedSpinnerView,
        dateSetListener: OnDateSetListener
    ): DatePickerDialog {
        val dateString = if (spinnerEditText.getText().isNotBlank())
            dateUtils.formatToYYYYmmDD(spinnerEditText.getText())
        else
            dateUtils.formatToYYYYmmDD(Date().formatToMMMddYYYY())
        val (year, month, day) = dateString?.split("-").orEmpty()
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
                    binding.productTaxStatus.setText(getString(ProductTaxStatus.toStringResource(it)))
                    viewModel.onDataChanged(taxStatus = ProductTaxStatus.fromString(it))
                }
            }
            RequestCodes.PRODUCT_TAX_CLASS -> {
                selectedItem?.let { selectedTaxClass ->
                    // Fetch the display name of the selected tax class slug
                    val selectedProductTaxClass = viewModel.getTaxClassBySlug(selectedTaxClass)
                    selectedProductTaxClass?.let {
                        binding.productTaxClass.setText(it.name)
                        viewModel.onDataChanged(taxClass = it.slug)
                    }
                }
            }
        }
    }
}
