package com.woocommerce.android.ui.orders.creation.addcustomer

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.databinding.FragmentOrderCreationAddCustomerBinding
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.creation.common.base.BaseOrderCreationFragment
import com.woocommerce.android.ui.orders.creation.common.navigation.OrderCreationNavigator
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddCustomerFragment : BaseOrderCreationFragment(layout.fragment_order_creation_add_customer) {
    @Inject lateinit var navigator: OrderCreationNavigator
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var customersAdapter: AddCustomerAdapter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: AddCustomerViewModel by viewModels { viewModelFactory }

    private var _binding: FragmentOrderCreationAddCustomerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderCreationAddCustomerBinding.bind(view)

        initObservers()
        initList()
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_add_customer_title)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initObservers() {
        viewModel.isFetchingFirstPage.observe(viewLifecycleOwner, Observer {
            binding.srlCustomers.isRefreshing = it == true
        })

        viewModel.isLoadingMore.observe(viewLifecycleOwner, Observer {
            it?.let { isLoadingMore ->
                binding.pbCustomers.isVisible = isLoadingMore
            }
        })

        viewModel.pagedListData.observe(viewLifecycleOwner, Observer {
            customersAdapter.submitList(it)
        })

        viewModel.emptyViewType.observe(viewLifecycleOwner, Observer { type ->
            when (type) {
                EmptyViewType.CUSTOMER_LIST -> binding.evCustomers.show(type)
                EmptyViewType.NETWORK_ERROR -> binding.evCustomers.show(type) { viewModel.onRefresh() }
                else -> binding.evCustomers.hide()
            }
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowErrorSnack -> {
                    uiMessageResolver.showSnack(event.messageRes)
                    binding.srlCustomers.isRefreshing = false
                }
                else -> event.isHandled = false
            }
        })
    }

    private fun initList() {
        binding.srlCustomers.setOnRefreshListener { viewModel.onRefresh() }

        with(binding.rvCustomers) {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = customersAdapter
            addItemDecoration(
                AlignedDividerDecoration(
                    ctx = context,
                    orientation = DividerItemDecoration.VERTICAL,
                    alignStartToStartOf = R.id.tvCustomerName
                )
            )
        }
    }
}
