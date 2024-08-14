package com.woocommerce.android.ui.payments.refunds

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentRefundByItemsBinding
import com.woocommerce.android.databinding.RefundByItemsFeesBinding
import com.woocommerce.android.databinding.RefundByItemsProductsBinding
import com.woocommerce.android.databinding.RefundByItemsShippingBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.OpenUrl
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundAmountDialog
import com.woocommerce.android.ui.payments.refunds.RefundFeeListAdapter.OnFeeLineCheckedChangeListener
import com.woocommerce.android.ui.payments.refunds.RefundShippingListAdapter.OnCheckedChangeListener
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class RefundByItemsFragment :
    BaseFragment(R.layout.fragment_refund_by_items),
    OnCheckedChangeListener,
    OnFeeLineCheckedChangeListener {
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    @Inject lateinit var imageMap: ProductImageMap

    private var _binding: FragmentRefundByItemsBinding? = null
    private val binding get() = _binding!!

    private var _productsBinding: RefundByItemsProductsBinding? = null
    private val productsBinding get() = _productsBinding!!

    private var _shippingLinesBinding: RefundByItemsShippingBinding? = null
    private val shippingLinesBinding get() = _shippingLinesBinding!!

    private var _feeLinesBinding: RefundByItemsFeesBinding? = null
    private val feeLinesBinding get() = _feeLinesBinding!!

    private val viewModel: IssueRefundViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_refunds)

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden
    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRefundByItemsBinding.bind(view)
        _productsBinding = binding.issueRefundProductsList
        _shippingLinesBinding = binding.issueRefundShippingSection
        _feeLinesBinding = binding.issueRefundFeesSection

        initializeViews()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _productsBinding = null
        _shippingLinesBinding = null
        _feeLinesBinding = null
    }

    private fun initializeViews() {
        productsBinding.issueRefundProducts.layoutManager = LinearLayoutManager(context)
        productsBinding.issueRefundProducts.setHasFixedSize(true)
        productsBinding.issueRefundProducts.isMotionEventSplittingEnabled = false

        shippingLinesBinding.issueRefundShippingLines.layoutManager = LinearLayoutManager(context)
        shippingLinesBinding.issueRefundShippingLines.setHasFixedSize(true)

        feeLinesBinding.issueRefundFeeLines.layoutManager = LinearLayoutManager(context)
        feeLinesBinding.issueRefundFeeLines.setHasFixedSize(true)

        binding.issueRefundSelectButton.setOnClickListener {
            viewModel.onSelectButtonTapped()
        }

        binding.issueRefundBtnNextFromItems.setOnClickListener {
            viewModel.onNextButtonTappedFromItems()
        }

        binding.issueRefundShippingMainSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            viewModel.onShippingRefundMainSwitchChanged(isChecked)
            binding.issueRefundShippingSection.root.isVisible = isChecked
        }

        binding.issueRefundFeesMainSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            viewModel.onFeesRefundMainSwitchChanged(isChecked)
            binding.issueRefundFeesSection.root.isVisible = isChecked
        }
    }

    private fun setupObservers() {
        viewModel.refundByItemsStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.currency?.takeIfNotEqualTo(old?.currency) {
                productsBinding.issueRefundProducts.adapter = RefundProductListAdapter(
                    currencyFormatter.buildBigDecimalFormatter(new.currency),
                    imageMap,
                    false,
                    { uniqueId -> viewModel.onRefundQuantityTapped(uniqueId) }
                )
                shippingLinesBinding.issueRefundShippingLines.adapter = RefundShippingListAdapter(
                    this,
                    currencyFormatter.buildBigDecimalFormatter(new.currency)
                )
                feeLinesBinding.issueRefundFeeLines.adapter = RefundFeeListAdapter(
                    this,
                    currencyFormatter.buildBigDecimalFormatter(new.currency)
                )
            }
            new.isNextButtonEnabled.takeIfNotEqualTo(old?.isNextButtonEnabled) {
                binding.issueRefundBtnNextFromItems.isEnabled = it
            }
            new.formattedProductsRefund?.takeIfNotEqualTo(old?.formattedProductsRefund) {
                productsBinding.issueRefundProductsTotal.text = it
            }
            new.shippingSubtotal?.takeIfNotEqualTo(old?.shippingSubtotal) {
                shippingLinesBinding.issueRefundShippingSubtotal.text = it
            }
            new.shippingTaxes?.takeIfNotEqualTo(old?.shippingTaxes) {
                shippingLinesBinding.issueRefundShippingTax.text = it
            }
            new.feesSubtotal?.takeIfNotEqualTo(old?.feesSubtotal) {
                feeLinesBinding.issueRefundFeesSubtotal.text = it
            }
            new.feesTaxes?.takeIfNotEqualTo(old?.feesTaxes) {
                feeLinesBinding.issueRefundFeesTax.text = it
            }
            new.selectedItemsHeader?.takeIfNotEqualTo(old?.selectedItemsHeader) {
                binding.issueRefundSelectedItems.text = it
            }
            new.selectButtonTitle?.takeIfNotEqualTo(old?.selectButtonTitle) {
                binding.issueRefundSelectButton.text = it
            }
            new.isShippingMainSwitchChecked.takeIfNotEqualTo(old?.isShippingMainSwitchChecked) { checked ->
                binding.issueRefundShippingMainSwitch.isChecked = checked
            }
            new.selectedShippingLines?.takeIfNotEqualTo(old?.selectedShippingLines) { shippingLines ->
                val adapter = shippingLinesBinding.issueRefundShippingLines.adapter as RefundShippingListAdapter
                adapter.updateToggleStates(shippingLines)

                if (shippingLines.isEmpty()) {
                    viewModel.onShippingRefundMainSwitchChanged(isChecked = false)
                    binding.issueRefundShippingSection.root.hide()
                }
            }
            new.isShippingRefundAvailable?.takeIfNotEqualTo(old?.isShippingRefundAvailable) { isVisible ->
                binding.issueRefundShippingContainer.isVisible = isVisible
            }
            new.formattedShippingRefundTotal?.takeIfNotEqualTo(old?.formattedShippingRefundTotal) {
                shippingLinesBinding.issueRefundShippingTotal.text = it
            }
            new.isFeesMainSwitchChecked.takeIfNotEqualTo(old?.isFeesMainSwitchChecked) { checked ->
                binding.issueRefundFeesMainSwitch.isChecked = checked
            }
            new.selectedFeeLines?.takeIfNotEqualTo(old?.selectedFeeLines) { feeLines ->
                val adapter = feeLinesBinding.issueRefundFeeLines.adapter as RefundFeeListAdapter
                adapter.updateToggleStates(feeLines)

                if (feeLines.isEmpty()) {
                    viewModel.onFeesRefundMainSwitchChanged(isChecked = false)
                    binding.issueRefundFeesSection.root.hide()
                }
            }
            new.isFeesRefundAvailable?.takeIfNotEqualTo(old?.isFeesRefundAvailable) { isVisible ->
                binding.issueRefundFeesContainer.isVisible = isVisible
            }
            new.formattedFeesRefundTotal?.takeIfNotEqualTo(old?.formattedFeesRefundTotal) {
                feeLinesBinding.issueRefundFeesTotal.text = it
            }
            new.isRefundNoticeVisible.takeIfNotEqualTo(old?.isRefundNoticeVisible) { isVisible ->
                if (isVisible) {
                    productsBinding.issueRefundRefundNotice.show()
                    productsBinding.issueRefundDividerBelowList.show()
                } else {
                    productsBinding.issueRefundRefundNotice.hide()
                    productsBinding.issueRefundDividerBelowList.hide()
                }
            }
            new.refundNotice.takeIfNotEqualTo(old?.refundNotice) { notice ->
                notice?.let { updateRefundNoticeView(it) }
            }
        }

        viewModel.refundItems.observe(viewLifecycleOwner) { list ->
            val adapter = productsBinding.issueRefundProducts.adapter as RefundProductListAdapter
            adapter.update(list)
        }

        viewModel.refundShippingLines.observe(viewLifecycleOwner) { list ->
            val adapter = shippingLinesBinding.issueRefundShippingLines.adapter as RefundShippingListAdapter
            adapter.update(list)
        }

        viewModel.refundFeeLines.observe(viewLifecycleOwner) { list ->
            val adapter = feeLinesBinding.issueRefundFeeLines.adapter as RefundFeeListAdapter
            adapter.update(list)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowNumberPicker -> {
                    val action = IssueRefundFragmentDirections.actionIssueRefundFragmentToRefundItemsPickerDialog(
                        title = getString(R.string.order_refunds_select_quantity),
                        uniqueId = event.refundItem.orderItem.itemId,
                        maxValue = event.refundItem.availableRefundQuantity,
                        currentValue = event.refundItem.quantity
                    )
                    findNavController().navigateSafely(action)
                }
                is ShowRefundAmountDialog -> {
                    val action = IssueRefundFragmentDirections.actionIssueRefundFragmentToRefundAmountDialog(
                        title = getString(R.string.order_refunds_products_refund),
                        maxValue = event.maxRefund,
                        currentValue = event.refundAmount,
                        minValue = BigDecimal.ZERO,
                        message = event.message
                    )
                    findNavController().navigateSafely(action)
                }
                is OpenUrl -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                else -> event.isHandled = false
            }
        }
    }

    private fun updateRefundNoticeView(refundNoticeText: String) {
        val linkText = getString(R.string.order_refunds_store_admin_link_text)
        val noticeText = "$refundNoticeText $linkText"
        val spannable = SpannableString(noticeText)
        val span = WooClickableSpan { viewModel.onOpenStoreAdminLinkClicked() }
        span.useCustomStyle = false
        spannable.setSpan(
            span,
            (noticeText.length - linkText.length),
            noticeText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        productsBinding.issueRefundRefundNotice.setText(spannable, TextView.BufferType.SPANNABLE)
        productsBinding.issueRefundRefundNotice.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onShippingLineSwitchChanged(isChecked: Boolean, itemId: Long) {
        viewModel.onShippingLineSwitchChanged(isChecked, itemId)
    }

    override fun onFeeLineSwitchChanged(isChecked: Boolean, itemId: Long) {
        viewModel.onFeeLineSwitchChanged(isChecked, itemId)
    }
}
