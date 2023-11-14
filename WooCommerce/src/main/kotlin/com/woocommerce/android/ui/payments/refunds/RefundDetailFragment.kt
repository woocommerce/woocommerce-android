package com.woocommerce.android.ui.payments.refunds

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentRefundDetailBinding
import com.woocommerce.android.databinding.RefundByItemsProductsBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.payments.refunds.RefundDetailViewModel.ViewOrderedAddons
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RefundDetailFragment : BaseFragment(R.layout.fragment_refund_detail) {
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var imageMap: ProductImageMap

    private val viewModel: RefundDetailViewModel by viewModels()

    private var _binding: FragmentRefundDetailBinding? = null
    private val binding get() = _binding!!

    private var _productsBinding: RefundByItemsProductsBinding? = null
    private val productsBinding get() = _productsBinding!!

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRefundDetailBinding.bind(view)
        _productsBinding = binding.issueRefundProductsList

        initializeViews()
        setupObservers(viewModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _productsBinding = null
    }

    private fun initializeViews() {
        productsBinding.issueRefundProducts.layoutManager = LinearLayoutManager(context)
        productsBinding.issueRefundProducts.setHasFixedSize(true)
    }

    @Suppress("LongMethod")
    private fun setupObservers(viewModel: RefundDetailViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.screenTitle?.takeIfNotEqualTo(old?.screenTitle) {
                activity?.title = new.screenTitle
            }
            new.refundAmount?.takeIfNotEqualTo(old?.refundAmount) {
                binding.refundDetailRefundAmount.text = new.refundAmount
                productsBinding.issueRefundProductsTotal.text = new.refundAmount
            }
            new.subtotal?.takeIfNotEqualTo(old?.subtotal) {
                productsBinding.issueRefundSubtotal.text = new.subtotal
            }
            new.taxes?.takeIfNotEqualTo(old?.taxes) {
                productsBinding.issueRefundTaxesTotal.text = new.taxes
            }
            new.refundMethod?.takeIfNotEqualTo(old?.refundMethod) {
                binding.refundDetailRefundMethod.text = new.refundMethod
            }
            new.currency?.takeIfNotEqualTo(old?.currency) {
                productsBinding.issueRefundProducts.adapter = RefundProductListAdapter(
                    currencyFormatter.buildBigDecimalFormatter(new.currency),
                    imageMap,
                    isProductDetailList = true,
                    onItemClicked = { uniqueId ->
                        (activity as? MainNavigationRouter)?.showProductDetail(uniqueId)
                    },
                    onViewAddonsClick = viewModel::onViewOrderedAddonButtonTapped
                )
            }
            new.areItemsVisible?.takeIfNotEqualTo(old?.areItemsVisible) { isVisible ->
                if (isVisible) {
                    binding.refundDetailRefundItems.show()
                } else {
                    binding.refundDetailRefundItems.hide()
                }
            }
            new.areDetailsVisible?.takeIfNotEqualTo(old?.areDetailsVisible) { isVisible ->
                if (isVisible) {
                    binding.refundDetailDetailsCard.show()
                    binding.refundDetailReasonCard.show()
                    productsBinding.issueRefundTotalsGroup.show()
                } else {
                    binding.refundDetailDetailsCard.hide()
                    binding.refundDetailReasonCard.hide()
                    productsBinding.issueRefundTotalsGroup.hide()
                }
            }

            if (new.refundReason.isNullOrEmpty()) {
                binding.refundDetailReasonCard.hide()
            } else {
                binding.refundDetailReasonCard.show()
                binding.refundDetailRefundReason.text = new.refundReason
            }
        }

        viewModel.refundItems.observe(viewLifecycleOwner) { list ->
            val adapter = productsBinding.issueRefundProducts.adapter as RefundProductListAdapter
            adapter.update(list)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ViewOrderedAddons ->
                    RefundDetailFragmentDirections
                        .actionRefundDetailFragmentToOrderedAddonFragment(
                            orderId = event.remoteOrderID,
                            orderItemId = event.orderItemID,
                            addonsProductId = event.addonsProductID
                        ).let { findNavController().navigateSafely(it) }
                else -> event.isHandled = false
            }
        }
    }
}
