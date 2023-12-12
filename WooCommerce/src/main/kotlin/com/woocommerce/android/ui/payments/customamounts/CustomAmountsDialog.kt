package com.woocommerce.android.ui.payments.customamounts

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogCustomAmountsBinding
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.CustomAmountUIModel
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.payments.PaymentsBaseDialogFragment
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialogViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialogViewModel.CustomAmountType.PERCENTAGE_CUSTOM_AMOUNT
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialogViewModel.PopulatePercentage
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialogViewModel.TaxStatus
import com.woocommerce.android.ui.payments.customamounts.views.TaxToggle
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.getDensityPixel
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DisplayUtils
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@AndroidEntryPoint
class CustomAmountsDialog : PaymentsBaseDialogFragment(R.layout.dialog_custom_amounts) {
    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: CustomAmountsDialogViewModel by viewModels()
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)
    private val arguments: CustomAmountsDialogArgs by navArgs()
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = ComponentDialog(requireContext(), theme)
        dialog.onBackPressedDispatcher.addCallback(dialog) {
            cancelDialog()
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isLandscape = DisplayUtils.isLandscape(requireContext())
        setWindowLayout(isLandscape)
        val binding = DialogCustomAmountsBinding.bind(view)
        bindViews(binding)
        setupClickListeners(binding)
        showKeyboard(isLandscape, binding)
        setupObservers(binding)
        setupEventObservers(binding)
    }

    private fun setWindowLayout(isLandscape: Boolean) {
        requireDialog().window?.let { window ->
            window.attributes?.windowAnimations = R.style.Woo_Animations_Dialog
            val widthRatio = if (isLandscape) WIDTH_RATIO_LANDSCAPE else WIDTH_RATIO
            val heightRatio = if (isLandscape) HEIGHT_RATIO_LANDSCAPE else HEIGHT_RATIO

            window.setLayout(
                (DisplayUtils.getWindowPixelWidth(requireContext()) * widthRatio).toInt(),
                (DisplayUtils.getWindowPixelHeight(requireContext()) * heightRatio).toInt()
            )
        }
    }

    private fun showKeyboard(
        isLandscape: Boolean,
        binding: DialogCustomAmountsBinding
    ) {
        when (arguments.customAmountUIModel.type) {
            FIXED_CUSTOM_AMOUNT -> {
                if (!isLandscape && binding.editPrice.editText.requestFocus()) {
                    binding.editPrice.postDelayed(
                        {
                            ActivityUtils.showKeyboard(binding.editPrice.editText)
                        },
                        KEYBOARD_DELAY
                    )
                }
            }
            PERCENTAGE_CUSTOM_AMOUNT -> {
                if (!isLandscape && binding.editPercentage.requestFocus()) {
                    binding.editPercentage.postDelayed(
                        {
                            ActivityUtils.showKeyboard(binding.editPercentage)
                        },
                        KEYBOARD_DELAY
                    )
                }
            }
        }
    }

    private fun bindViews(binding: DialogCustomAmountsBinding) {
        bindAmountsView(binding)
        bindPercentageLabel(binding)
        setupTaxToggleView(binding)
        setupPrimaryEditView(binding)
        setupDeleteCustomAmountView(binding)
    }

    private fun setupDeleteCustomAmountView(binding: DialogCustomAmountsBinding) {
        if (viewModel.isInCreateMode()) {
            binding.buttonDelete.hide()
        } else {
            binding.buttonDelete.show()
        }
    }

    private fun bindPercentageLabel(binding: DialogCustomAmountsBinding) {
        with(binding.percentageLabel) {
            text = String.format(
                context.getString(R.string.custom_amounts_percentage_label, arguments.orderTotal)
            )
        }
    }

    private fun bindAmountsView(binding: DialogCustomAmountsBinding) {
        binding.editPrice.editText.setPadding(
            getDensityPixel(binding.editPrice.context, START_PADDING), 0, 0, 0
        )
    }

    private fun setupClickListeners(binding: DialogCustomAmountsBinding) {
        binding.buttonDone.setOnClickListener {
            sharedViewModel.onCustomAmountUpsert(
                CustomAmountUIModel(
                    id = viewModel.viewState.customAmountUIModel.id,
                    amount = viewModel.viewState.customAmountUIModel.currentPrice,
                    name = viewModel.viewState.customAmountUIModel.name,
                    taxStatus = viewModel.viewState.customAmountUIModel.taxStatus,
                    type = viewModel.viewState.customAmountUIModel.type
                )
            )
        }
        binding.buttonDelete.setOnClickListener {
            sharedViewModel.onCustomAmountRemoved(
                CustomAmountUIModel(
                    id = viewModel.viewState.customAmountUIModel.id,
                    amount = viewModel.viewState.customAmountUIModel.currentPrice,
                    name = viewModel.viewState.customAmountUIModel.name,
                    taxStatus = viewModel.viewState.customAmountUIModel.taxStatus,
                    type = viewModel.viewState.customAmountUIModel.type
                )
            )
        }
        binding.imageClose.setOnClickListener {
            cancelDialog()
        }
    }

    private fun setupEventObservers(binding: DialogCustomAmountsBinding) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is PopulatePercentage -> {
                    binding.editPercentage.setText(
                        viewModel.currentPercentage.setScale(2, RoundingMode.HALF_UP).toString()
                    )
                }
            }
        }
    }

    private fun setupPrimaryEditView(binding: DialogCustomAmountsBinding) {
        when (arguments.customAmountUIModel.type) {
            FIXED_CUSTOM_AMOUNT -> {
                binding.editPrice.show()
                binding.groupPercentage.hide()
                binding.percentageLabel.hide()
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        binding.editPrice.value.filterNotNull().observe(
                            this
                        ) {
                            viewModel.currentPrice = it
                        }
                    },
                    EDIT_PRICE_UPDATE_DELAY
                )
            }

            PERCENTAGE_CUSTOM_AMOUNT -> {
                binding.editPrice.hide()
                binding.groupPercentage.show()
                binding.percentageLabel.show()
            }
        }
    }

    private fun setupTaxToggleView(binding: DialogCustomAmountsBinding) {
        binding.taxToggleComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    val viewState = viewModel.viewStateLiveData.liveData.observeAsState()
                    val taxToggleState = remember { mutableStateOf(false) }
                    TaxToggle(taxStatus = viewState.value?.customAmountUIModel?.taxStatus ?: TaxStatus()) { isChecked ->
                        taxToggleState.value = isChecked
                        viewModel.taxToggleState = viewModel.taxToggleState.copy(
                            isTaxable = isChecked
                        )
                    }
                }
            }
        }
    }

    private fun setupObservers(binding: DialogCustomAmountsBinding) {
        observePercentageView(binding)
        observeCustomAmountNameView(binding)

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isDoneButtonEnabled.takeIfNotEqualTo(old?.isDoneButtonEnabled) { isEnabled ->
                binding.buttonDone.isEnabled = isEnabled
            }

            new.isProgressShowing.takeIfNotEqualTo(old?.isProgressShowing) { show ->
                binding.progressBar.isVisible = show
                binding.buttonDone.text = if (show) "" else getString(R.string.custom_amounts_add_custom_amount)
            }
            new.customAmountUIModel.takeIfNotEqualTo(old?.customAmountUIModel) {
                if (binding.customAmountNameText.text.toString() != it.name) {
                    binding.customAmountNameText.setText(it.name)
                    binding.customAmountNameText.setSelection(it.name.length)
                }
                when (arguments.customAmountUIModel.type) {
                    FIXED_CUSTOM_AMOUNT -> {
                        if (binding.editPrice.editText.text.toString() != it.currentPrice.toString()) {
                            binding.editPrice.setValue(it.currentPrice)
                            binding.editPrice.editText.setSelection(binding.editPrice.editText.text?.length ?: 0)
                        }
                    }

                    PERCENTAGE_CUSTOM_AMOUNT -> {
                        binding.updatedAmount.text = viewModel.currentPrice.toString()
                    }
                }
            }
        }
    }

    private fun observeCustomAmountNameView(binding: DialogCustomAmountsBinding) {
        binding.customAmountNameText.addTextChangedListener {
            viewModel.currentName = it.toString()
        }
    }

    private fun observePercentageView(binding: DialogCustomAmountsBinding) {
        if (arguments.customAmountUIModel.type == PERCENTAGE_CUSTOM_AMOUNT) {
            binding.editPercentage.addTextChangedListener {
                if (it != null && it.toString().isNotEmpty()) {
                    if (it.toString() != viewModel.currentPercentage.toString()) {
                        viewModel.currentPercentage = BigDecimal(it.toString())
                    }
                    binding.updatedAmount.show()
                } else {
                    binding.updatedAmount.hide()
                }
            }
        }
    }

    private fun cancelDialog() {
        findNavController().navigateUp()
    }

    companion object {
        private const val HEIGHT_RATIO = 0.6
        private const val WIDTH_RATIO = 0.9
        private const val HEIGHT_RATIO_LANDSCAPE = 0.9
        private const val WIDTH_RATIO_LANDSCAPE = 0.6
        private const val KEYBOARD_DELAY = 500L

        const val CUSTOM_AMOUNT = "Custom Amount"
        const val EDIT_PRICE_UPDATE_DELAY = 100L
        const val START_PADDING = 8
    }
}
