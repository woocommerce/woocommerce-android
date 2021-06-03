package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentPrintLabelCustomsFormBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrintShippingLabelCustomsFormFragment : BaseFragment(R.layout.fragment_print_label_customs_form) {
    private val viewModel: PrintShippingLabelCustomsFormViewModel by viewModels()

    override fun getFragmentTitle(): String = getString(R.string.shipping_label_print_customs_form_screen_title)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPrintLabelCustomsFormBinding.bind(view)
        setupObservers()
        setupView(binding)
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
    }

    private fun setupView(binding: FragmentPrintLabelCustomsFormBinding) {
        binding.printButton.setOnClickListener {
            viewModel.onPrintButtonClicked()
        }
        binding.saveForLaterButton.setOnClickListener {
            viewModel.onSaveForLaterClicked()
        }
    }
}
