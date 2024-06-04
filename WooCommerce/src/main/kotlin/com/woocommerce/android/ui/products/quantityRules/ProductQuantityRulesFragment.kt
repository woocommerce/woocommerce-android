package com.woocommerce.android.ui.products.quantityRules

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentProductQuantityRulesBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.products.BaseProductEditorFragment
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitProductQuantityRules
import com.woocommerce.android.ui.products.shipping.ProductShippingViewModel
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductQuantityRulesFragment : BaseProductEditorFragment(R.layout.fragment_product_quantity_rules) {
    private val viewModel: ProductQuantityRulesViewModel by viewModels()

    override val lastEvent: MultiLiveEvent.Event?
        get() = viewModel.event.value

    companion object {
        val TAG: String = ProductQuantityRulesFragment::class.java.simpleName
    }

    private val navArgs: ProductQuantityRulesFragmentArgs by navArgs()

    private var _binding: FragmentProductQuantityRulesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductQuantityRulesBinding.bind(view)
        //viewModel.event.observe(viewLifecycleOwner, Observer(::onEventReceived))
        setupObservers(viewModel)
        initializeViews()

        setupViews()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers(viewModel: ProductQuantityRulesViewModel) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is MultiLiveEvent.Event.ExitWithResult<*> -> navigateBackWithResult(
                    KEY_QUANTITY_RULES_DIALOG_RESULT,
                    event.data
                )
                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
                is MultiLiveEvent.Event.ShowDialog -> event.showDialog()
                else -> event.isHandled = false
            }
        }
    }

    private fun initializeViews() {
        binding.minQuantity.text = viewModel.quantityRules.min?.toString() ?: getString(R.string.empty_min_quantity)
        binding.maxQuantity.text = viewModel.quantityRules.max?.toString() ?: getString(R.string.empty_max_quantity)
        binding.groupOf.text = viewModel.quantityRules.groupOf?.toString() ?: getString(R.string.empty_group_of)
    }

    private fun setupViews() {
        binding.minQuantity.setOnTextChangedListener {
            viewModel.onDataChanged(min = it?.toString()?.toIntOrNull())
        }

        binding.maxQuantity.setOnTextChangedListener {
            viewModel.onDataChanged(max = it?.toString()?.toIntOrNull())
        }

        binding.groupOf.setOnTextChangedListener {
            viewModel.onDataChanged(groupOf = it?.toString()?.toIntOrNull())
        }

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_quantity_rules_title),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    onExit()
                }
            }
        )
    }

    override fun onExit() {
        viewModel.onExit()
    }
}
