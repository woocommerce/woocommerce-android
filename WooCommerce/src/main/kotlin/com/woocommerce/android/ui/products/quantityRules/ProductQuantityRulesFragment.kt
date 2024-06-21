package com.woocommerce.android.ui.products.quantityRules

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentProductQuantityRulesBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.products.BaseProductEditorFragment
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

    private var _binding: FragmentProductQuantityRulesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductQuantityRulesBinding.bind(view)
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
        binding.minQuantity.text = quantityToString(viewModel.quantityRules.min) ?: ""
        binding.maxQuantity.text = quantityToString(viewModel.quantityRules.max) ?: ""
        binding.groupOf.text = quantityToString(viewModel.quantityRules.groupOf) ?: ""
    }

    private fun quantityToString(quantity: Int?): String? {
        return quantity?.let {
            if (it > 0) it.toString() else null
        }
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
