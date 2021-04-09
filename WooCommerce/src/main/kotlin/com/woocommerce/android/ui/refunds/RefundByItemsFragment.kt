package com.woocommerce.android.ui.refunds

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentRefundByItemsBinding
import com.woocommerce.android.databinding.RefundByItemsProductsBinding
import com.woocommerce.android.databinding.RefundByItemsShippingBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.OpenUrl
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundAmountDialog
import com.woocommerce.android.ui.refunds.RefundShippingListAdapter.OnCheckedChangeListener
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.Lazy
import java.math.BigDecimal
import javax.inject.Inject

class RefundByItemsFragment : BaseFragment(R.layout.fragment_refund_by_items),
    OnCheckedChangeListener {
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var imageMap: ProductImageMap

    private var _binding: FragmentRefundByItemsBinding? = null
    private val binding get() = _binding!!

    private var _productsBinding: RefundByItemsProductsBinding? = null
    private val productsBinding get() = _productsBinding!!

    private var _shippingLinesBinding: RefundByItemsShippingBinding? = null
    private val shippingLinesBinding get() = _shippingLinesBinding!!

    private val viewModel: IssueRefundViewModel by navGraphViewModels(R.id.nav_graph_refunds) {
        viewModelFactory.get()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRefundByItemsBinding.bind(view)
        _productsBinding = binding.issueRefundProductsList
        _shippingLinesBinding = binding.issueRefundShippingSection

        initializeViews()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _productsBinding = null
        _shippingLinesBinding = null
    }

    private fun initializeViews() {
        productsBinding.issueRefundProducts.layoutManager = LinearLayoutManager(context)
        productsBinding.issueRefundProducts.setHasFixedSize(true)
        productsBinding.issueRefundProducts.isMotionEventSplittingEnabled = false

        shippingLinesBinding.issueRefundShippingLines.layoutManager = LinearLayoutManager(context)
        shippingLinesBinding.issueRefundShippingLines.setHasFixedSize(true)

        binding.issueRefundSelectButton.setOnClickListener {
            viewModel.onSelectButtonTapped()
        }

        binding.issueRefundBtnNextFromItems.setOnClickListener {
            viewModel.onNextButtonTappedFromItems()
        }

        binding.issueRefundShippingMainSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            viewModel.onShippingRefundMainSwitchChanged(isChecked)
            if (isChecked)
                binding.issueRefundShippingSection.root.show()
            else
                binding.issueRefundShippingSection.root.hide()
        }

        // TODO: Temporarily disabled, this will be used in a future release - do not remove
//        issueRefund_productsTotal.setOnClickListener {
//            viewModel.onProductRefundAmountTapped()
//        }
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
                    currencyFormatter.buildBigDecimalFormatter(new.currency))
            }
            new.isNextButtonEnabled?.takeIfNotEqualTo(old?.isNextButtonEnabled) {
                binding.issueRefundBtnNextFromItems.isEnabled = it
            }
            new.formattedProductsRefund?.takeIfNotEqualTo(old?.formattedProductsRefund) {
                productsBinding.issueRefundProductsTotal.text = it
            }
            new.shippingSubtotal?.takeIfNotEqualTo(old?.shippingSubtotal) {
                shippingLinesBinding.issueRefundShippingSubtotal.text = it
            }
            new.shippingTaxes?.takeIfNotEqualTo(old?.shippingSubtotal) {
                shippingLinesBinding.issueRefundShippingTax.text = it
            }
            new.feesTotal?.takeIfNotEqualTo(old?.feesTotal) {
                productsBinding.issueRefundFeesTotal.text = it
            }
            new.taxes?.takeIfNotEqualTo(old?.taxes) {
                productsBinding.issueRefundTaxesTotal.text = it
            }
            new.subtotal?.takeIfNotEqualTo(old?.subtotal) {
                productsBinding.issueRefundSubtotal.text = it
            }
            new.selectedItemsHeader?.takeIfNotEqualTo(old?.selectedItemsHeader) {
                binding.issueRefundSelectedItems.text = it
            }
            new.selectButtonTitle?.takeIfNotEqualTo(old?.selectButtonTitle) {
                binding.issueRefundSelectButton.text = it
            }
            new.isShippingMainSwitchChecked?.takeIfNotEqualTo(old?.isShippingMainSwitchChecked) { checked ->
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
                if (isVisible) {
                    binding.issueRefundShippingContainer.show()
                } else {
                    binding.issueRefundShippingContainer.hide()
                }
            }
            new.formattedShippingRefundTotal?.takeIfNotEqualTo(old?.formattedShippingRefundTotal) {
                shippingLinesBinding.issueRefundShippingTotal.text = it
            }
            new.isRefundNoticeVisible.takeIfNotEqualTo(old?.isRefundNoticeVisible) { isVisible ->
                if (isVisible) {
                    productsBinding.issueRefundRefundNotice.show()
                } else {
                    productsBinding.issueRefundRefundNotice.hide()
                }
            }
            new.refundNotice.takeIfNotEqualTo(old?.refundNotice) { notice ->
                notice?.let { updateRefundNoticeView(it) }
            }
            new.isFeesVisible?.takeIfNotEqualTo(old?.isFeesVisible) { isVisible ->
                if (isVisible) {
                    productsBinding.issueRefundFeesGroup.show()
                } else {
                    productsBinding.issueRefundFeesGroup.hide()
                }
            }
        }

        viewModel.refundItems.observe(viewLifecycleOwner, Observer { list ->
            val adapter = productsBinding.issueRefundProducts.adapter as RefundProductListAdapter
            adapter.update(list)
        })

        viewModel.refundShippingLines.observe(viewLifecycleOwner, Observer { list ->
            val adapter = shippingLinesBinding.issueRefundShippingLines.adapter as RefundShippingListAdapter
            adapter.update(list)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowNumberPicker -> {
                    val action = IssueRefundFragmentDirections.actionIssueRefundFragmentToRefundItemsPickerDialog(
                            getString(R.string.order_refunds_select_quantity),
                            event.refundItem.orderItem.uniqueId,
                            event.refundItem.maxQuantity,
                            event.refundItem.quantity
                    )
                    findNavController().navigateSafely(action)
                }
                is ShowRefundAmountDialog -> {
                    val action = IssueRefundFragmentDirections.actionIssueRefundFragmentToRefundAmountDialog(
                            getString(R.string.order_refunds_products_refund),
                            event.maxRefund,
                            event.refundAmount,
                            BigDecimal.ZERO,
                            event.message
                    )
                    findNavController().navigateSafely(action)
                }
                is OpenUrl -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                else -> event.isHandled = false
            }
        })
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
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        productsBinding.issueRefundRefundNotice.setText(spannable, TextView.BufferType.SPANNABLE)
        productsBinding.issueRefundRefundNotice.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onShippingLineSwitchChanged(isChecked: Boolean, itemId: Long) {
        viewModel.onShippingLineSwitchChanged(isChecked, itemId)
    }
}
