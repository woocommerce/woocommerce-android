package com.woocommerce.android.ui.orders.creation.neworder

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationNewOrderBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.common.navigation.OrderCreationNavigationTarget
import com.woocommerce.android.ui.orders.creation.common.navigation.OrderCreationNavigator
import com.woocommerce.android.util.setHomeIcon
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class NewOrderFragment : BaseFragment(R.layout.fragment_order_creation_new_order) {
    @Inject lateinit var navigator: OrderCreationNavigator
    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: NewOrderViewModel by viewModels { viewModelFactory }

    private var _binding: FragmentOrderCreationNewOrderBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderCreationNewOrderBinding.bind(view)

        setHomeIcon(R.drawable.ic_gridicons_cross_24dp)

        // The button is used for development purposes
        binding.addNewCustomerButton.setOnClickListener {
            viewModel.onAddNewCustomerButtonClicked()
        }

        initializeViewModel()
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_new_order_title)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeViewModel() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OrderCreationNavigationTarget -> navigator.navigate(this, event)
            }
        }
    }
}
