package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingCustomsBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShippingCustomsFragment : BaseFragment(R.layout.fragment_shipping_customs), BackPressListener {
    companion object {
        const val EDIT_CUSTOMS_CLOSED = "edit_customs_closed"
        const val EDIT_CUSTOMS_RESULT = "edit_customs_result"
    }

    private val viewModel: ShippingCustomsViewModel by viewModels()

    private val customsAdapter: ShippingCustomsAdapter by lazy {
        ShippingCustomsAdapter(viewModel)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.onDoneButtonClicked()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun getFragmentTitle(): String = getString(R.string.shipping_label_create_customs)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        val binding = FragmentShippingCustomsBinding.bind(view)
        binding.packagesList.apply {
            this.adapter = customsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            itemAnimator = DefaultItemAnimator().apply {
                // Disable change animations to avoid duplicating viewholders
                supportsChangeAnimations = false
            }
        }

        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentShippingCustomsBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner, { old, new ->
            new.customsPackages.takeIfNotEqualTo(old?.customsPackages) { customsPackages ->
                customsAdapter.customsPackages = customsPackages
            }
        })
        viewModel.event.observe(viewLifecycleOwner, { event ->
            when (event) {
                // TODO use EDIT_CUSTOMS_CLOSED for ExitWIthResult event, and EDIT_CUSTOMS_CLOSED for Exit
                is Exit -> navigateBackWithNotice(EDIT_CUSTOMS_RESULT)
                else -> event.isHandled = false
            }
        })
    }

    override fun onRequestAllowBackPress(): Boolean {
        // TODO pass this to the ViewModel
        navigateBackWithNotice(EDIT_CUSTOMS_RESULT)
        return false
    }
}
