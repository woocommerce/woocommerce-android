package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentEditShippingLabelPaymentBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.viewmodel.ViewModelFactory
import javax.inject.Inject

class EditShippingLabelPaymentFragment : BaseFragment(R.layout.fragment_edit_shipping_label_payment) {
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: EditShippingLabelPaymentViewModel by viewModels { viewModelFactory }

    private val paymentMethodsAdapter by lazy { ShippingLabelPaymentMethodsAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentEditShippingLabelPaymentBinding.bind(view)
        binding.paymentMethodsList.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = paymentMethodsAdapter
        }
        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentEditShippingLabelPaymentBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isLoading.takeIfNotEqualTo(old?.isLoading) { isLoading ->
                binding.loadingProgress.isVisible = isLoading
                binding.contentLayout.isVisible = !isLoading
            }
            new.paymentMethods.takeIfNotEqualTo(old?.paymentMethods) {
                paymentMethodsAdapter.items = it
            }
            new.emailReceipts.takeIfNotEqualTo(binding.emailReceiptsCheckbox.isChecked) {
                binding.emailReceiptsCheckbox.isChecked = it
            }
            new.storeOwnerDetails?.takeIfNotEqualTo(old?.storeOwnerDetails) { details ->
                binding.paymentsInfo.text = getString(
                    R.string.shipping_label_payments_account_info,
                    details.wpcomUserName,
                    details.wpcomEmail
                )
                binding.emailReceiptsCheckbox.text = getString(
                    R.string.shipping_label_payments_email_receipts_checkbox,
                    details.name.ifEmpty { details.userName },
                    details.userName,
                    details.wpcomEmail
                )
            }
        }
    }
}
